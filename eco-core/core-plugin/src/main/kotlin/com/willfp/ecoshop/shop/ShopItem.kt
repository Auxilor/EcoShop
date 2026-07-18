@file:JvmName("ShopItemUtils")

package com.willfp.ecoshop.shop

import com.willfp.eco.core.cache.EcoCache
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.data.keys.PersistentDataKey
import com.willfp.eco.core.data.keys.PersistentDataKeyType
import com.willfp.eco.core.data.profile
import com.willfp.eco.core.drops.DropQueue
import com.willfp.eco.core.fast.fast
import com.willfp.eco.core.items.HashedItem
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.price.CombinedDisplayPrice
import com.willfp.eco.core.price.ConfiguredPrice
import com.willfp.eco.core.registry.KRegistrable
import com.willfp.eco.util.NumberUtils
import com.willfp.eco.util.StringUtils
import com.willfp.eco.util.formatEco
import com.willfp.ecoshop.event.EcoShopBuyEvent
import com.willfp.ecoshop.event.EcoShopSellEvent
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.shop.gui.BuyMenu
import com.willfp.ecoshop.shop.gui.SellMenu
import com.willfp.ecoshop.shop.gui.ShopItemSlot
import com.willfp.libreforge.EmptyProvidedHolder
import com.willfp.libreforge.NamedValue
import com.willfp.libreforge.ViolationContext
import com.willfp.libreforge.conditions.Conditions
import com.willfp.libreforge.effects.Effects
import com.willfp.libreforge.toDispatcher
import com.willfp.libreforge.triggers.DispatchedTrigger
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.impl.TriggerBlank
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import java.util.Optional
import java.time.Duration

enum class BuyType {
    NORMAL,
    ALT
}

class ShopItem(
    val config: Config,
    categoryDynamicPricing: DynamicPricingConfig? = null
) : KRegistrable {
    override val id = config.getString("id")

    val dynamicPricing: DynamicPricingConfig? = run {
        val hasBuyDp = config.has("buy") && config.getSubsection("buy").has("dynamic-pricing")
        val hasAltBuyDp = config.has("alt-buy") && config.getSubsection("alt-buy").has("dynamic-pricing")
        val hasSellDp = config.has("sell") && config.getSubsection("sell").has("dynamic-pricing")

        if (!hasBuyDp && !hasAltBuyDp && !hasSellDp) return@run categoryDynamicPricing

        fun parseItemType(priceKey: String, parentType: PriceDynamicConfig?): PriceDynamicConfig {
            if (!config.has(priceKey)) return parentType ?: PriceDynamicConfig(false, 2.0, 0.0, null)
            val section = config.getSubsection(priceKey)
            if (!section.has("dynamic-pricing")) return parentType ?: PriceDynamicConfig(false, 2.0, 0.0, null)
            val dp = section.getSubsection("dynamic-pricing")
            val enabled = dp.getBoolOrNull("enabled") ?: parentType?.enabled ?: false
            val maxIncrease = dp.getDoubleOrNull("max-increase") ?: parentType?.maxIncrease ?: 2.0
            val maxDecrease = dp.getDoubleOrNull("max-decrease") ?: parentType?.maxDecrease ?: 0.0
            val formula = if (enabled)
                dp.getStringOrNull("formula", false, StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                    ?: parentType?.formula
            else null
            return PriceDynamicConfig(enabled, maxIncrease, maxDecrease, formula)
        }

        DynamicPricingConfig(
            buy = parseItemType("buy", categoryDynamicPricing?.buy),
            altBuy = parseItemType("alt-buy", categoryDynamicPricing?.altBuy),
            sell = parseItemType("sell", categoryDynamicPricing?.sell),
            decayEnabled = categoryDynamicPricing?.decayEnabled ?: false,
            decayRate = categoryDynamicPricing?.decayRate ?: 0.0,
            decayPeriodMinutes = categoryDynamicPricing?.decayPeriodMinutes ?: 1440
        )
    }

    private val warnedFormulas = mutableSetOf<String>()

    private val context = ViolationContext(plugin, "shop item $id")

    val item = if (config.has("item")) Items.lookup(config.getString("item")) else null

    val buyEffects = Effects.compileChain(
        listOf("effects", "buy-effects").flatMap { config.getSubsections(it) },
        context.with("buy-effects")
    )

    val sellEffects = Effects.compileChain(
        config.getSubsections("sell-effects"),
        context.with("sell-effects")
    )

    val buyAmount = config.getIntOrNull("buy.amount") ?: 1

    private val _displayItem = ItemStackBuilder(
        if (config.has("gui.display.item")) {
            Items.lookup(config.getString("gui.display.item")).item
        } else item?.item?.clone() ?: ItemStack(Material.AIR)
    )
        .setAmount(buyAmount)
        .addLoreLines(config.getStrings("gui.display.lore"))
        .build()

    val displayItem: ItemStack
        get() = this._displayItem.clone()

    // Formatted on render for placeholder support.
    val bottomLore = config.getStrings("gui.display.bottom-lore")

    private val _displayName = config.getFormattedStringOrNull("name")

    val displayName: String
        get() = _displayName ?: this.displayItem.fast().displayName

    val row = config.getInt("gui.row")

    val column = config.getInt("gui.column")

    val page = config.getInt("gui.page")

    val isBuyable = config.has("buy")

    val isSellable = config.has("sell")

    val hasAltBuy = config.has("alt-buy")

    val buyPrice = ConfiguredPrice.create(config.getSubsection("buy"))

    val altBuyPrice = ConfiguredPrice.create(config.getSubsection("alt-buy"))

    val sellPrice = ConfiguredPrice.create(config.getSubsection("sell"))

    val buyConditions = Conditions.compile(
        config.getSubsections("buy.conditions"),
        context.with("buy conditions")
    )

    val altBuyConditions = Conditions.compile(
        config.getSubsections("alt-buy.conditions"),
        context.with("alt buy conditions")
    )

    val sellConditions = Conditions.compile(
        config.getSubsections("sell.conditions"),
        context.with("sell conditions")
    )

    val isStrictMatch = plugin.configYml.getBoolOrNull("shop-items.sell-strict-match") ?: true

    val slot = ShopItemSlot(this)

    val isShowingQuickBuySell = config.getBoolOrNull("gui.show-quick-buy-sell") ?: true

    private val buyMenus = BuyType.entries.associateWith { BuyMenu(this, it) }

    val sellMenu = SellMenu(this)

    val limit = config.getIntOrNull("buy.limit") ?: Int.MAX_VALUE

    val globalLimit = config.getIntOrNull("buy.global-limit") ?: Int.MAX_VALUE

    val sellLimit = config.getIntOrNull("sell.limit") ?: Int.MAX_VALUE

    val globalSellLimit = config.getIntOrNull("sell.global-limit") ?: Int.MAX_VALUE

    private val maxAtOnce = config.getIntOrNull("buy.max-at-once") ?: Int.MAX_VALUE

    private val timesBoughtKey = PersistentDataKey(
        plugin.createNamespacedKey("${id}_times_bought"),
        PersistentDataKeyType.INT,
        0
    )

    private val timesSoldKey = PersistentDataKey(
        plugin.createNamespacedKey("${id}_times_sold"),
        PersistentDataKeyType.INT,
        0
    )

    private val dynamicBuysKey = PersistentDataKey(
        plugin.createNamespacedKey("${id}_dp_buys"),
        PersistentDataKeyType.INT,
        0
    )

    private val dynamicSellsKey = PersistentDataKey(
        plugin.createNamespacedKey("${id}_dp_sells"),
        PersistentDataKeyType.INT,
        0
    )

    private val lastDecayTimeKey = PersistentDataKey(
        plugin.createNamespacedKey("${id}_dp_last_decay"),
        PersistentDataKeyType.INT,
        0
    )

    init {
        if (this.item != null && this.item.item.amount != 1) {
            throw InvalidShopItemException(
                "Item $id is buying/selling an item " +
                        "with a stack size greater than 1. If you want to sell" +
                        "more than one item at a time, use the buy amount amount option."
            )
        }

        if (this.hasAltBuy) {
            if (this.isSellable) {
                throw InvalidShopItemException(
                    "Item $id cannot be both alt-buyable and sellable!"
                )
            }

            if (!this.isBuyable) {
                throw InvalidShopItemException(
                    "Item $id must be buyable in order to be alt buyable!"
                )
            }
        }

        if (plugin.configYml.getBool("shop-items.register-permissions")) {
            if (Bukkit.getPluginManager().getPermission("ecoshop.buy.$id") == null) {
                val permission = Permission(
                    "ecoshop.buy.$id",
                    "Allows buying $id",
                    PermissionDefault.TRUE
                )

                if (Bukkit.getPluginManager().getPermission("ecoshop.buy.*") == null) {
                    Bukkit.getPluginManager().addPermission(
                        Permission(
                            "ecoshop.buy.*",
                            "Allows buying all items from shops",
                            PermissionDefault.TRUE
                        )
                    )
                }

                permission.addParent(
                    Bukkit.getPluginManager().getPermission("ecoshop.buy.*")!!,
                    true
                )

                Bukkit.getPluginManager().addPermission(permission)
            }

            if (Bukkit.getPluginManager().getPermission("ecoshop.sell.$id") == null) {
                val permission = Permission(
                    "ecoshop.sell.$id",
                    "Allows selling $id",
                    PermissionDefault.TRUE
                )

                if (Bukkit.getPluginManager().getPermission("ecoshop.sell.*") == null) {
                    Bukkit.getPluginManager().addPermission(
                        Permission(
                            "ecoshop.sell.*",
                            "Allows selling all items to shops",
                            PermissionDefault.TRUE
                        )
                    )
                }

                permission.addParent(
                    Bukkit.getPluginManager().getPermission("ecoshop.sell.*")!!,
                    true
                )

                Bukkit.getPluginManager().addPermission(permission)
            }
        }
    }

    private fun effectiveBuyValue(buyType: BuyType, baseValue: Double): Double {
        val pc = when (buyType) {
            BuyType.NORMAL -> dynamicPricing?.buy
            BuyType.ALT -> dynamicPricing?.altBuy
        } ?: return baseValue
        if (!pc.enabled) return baseValue
        val formula = pc.formula ?: return baseValue

        val substituted = formula
            .replace("%base_price%", baseValue.toString())
            .replace("%buys%", getDynamicGlobalBuys().toString())
            .replace("%sells%", getDynamicGlobalSells().toString())

        val result = NumberUtils.evaluateExpression(substituted)
        if (result.isNaN() || result.isInfinite() || (result == 0.0 && baseValue != 0.0)) {
            if (warnedFormulas.add("$id:$formula")) {
                plugin.logger.warning("[EcoShop] Dynamic pricing formula failed for item '$id': \"$formula\"")
            }
            return baseValue
        }

        val clamped = result.coerceIn(
            minOf(baseValue * pc.maxDecrease, baseValue * pc.maxIncrease),
            maxOf(baseValue * pc.maxDecrease, baseValue * pc.maxIncrease)
        )
        return Math.round(clamped * 100) / 100.0
    }

    private fun effectiveSellValue(baseValue: Double): Double {
        val pc = dynamicPricing?.sell ?: return baseValue
        if (!pc.enabled) return baseValue
        val formula = pc.formula ?: return baseValue

        val substituted = formula
            .replace("%base_price%", baseValue.toString())
            .replace("%buys%", getDynamicGlobalBuys().toString())
            .replace("%sells%", getDynamicGlobalSells().toString())

        val result = NumberUtils.evaluateExpression(substituted)
        if (result.isNaN() || result.isInfinite() || (result == 0.0 && baseValue != 0.0)) {
            if (warnedFormulas.add("$id:$formula")) {
                plugin.logger.warning("[EcoShop] Dynamic pricing formula failed for item '$id': \"$formula\"")
            }
            return baseValue
        }

        val clamped = result.coerceIn(
            minOf(baseValue * pc.maxDecrease, baseValue * pc.maxIncrease),
            maxOf(baseValue * pc.maxDecrease, baseValue * pc.maxIncrease)
        )
        return Math.round(clamped * 100) / 100.0
    }

    fun getEffectiveBuyMultiplier(buyType: BuyType, player: Player): Double {
        val baseValue = getBuyPrice(buyType)?.getValue(player) ?: return 1.0
        if (baseValue <= 0) return 1.0
        return effectiveBuyValue(buyType, baseValue) / baseValue
    }

    fun getEffectiveSellMultiplier(player: Player): Double {
        val baseValue = sellPrice?.getValue(player) ?: return 1.0
        if (baseValue <= 0) return 1.0
        return effectiveSellValue(baseValue) / baseValue
    }

    /** Get the max amount of times this item can be bought at a single time. */
    fun getMaxBuysAtOnce(player: Player): Int {
        return maxAtOnce.coerceAtMost(getBuysLeft(player))
    }

    /** Get the max amount of times this player can buy this item again. */
    fun getBuysLeft(player: OfflinePlayer): Int {
        return limit - getTotalBuys(player)
    }

    /** Get the total amount of times a player has bought this item. */
    fun getTotalBuys(player: OfflinePlayer): Int {
        return player.profile.read(timesBoughtKey)
    }

    /** Get the total amount of times a server has bought this item. */
    fun getTotalGlobalBuys(): Int {
        return Bukkit.getServer().profile.read(timesBoughtKey)
    }

    private fun getDynamicGlobalBuys(): Int =
        Bukkit.getServer().profile.read(dynamicBuysKey)

    private fun getDynamicGlobalSells(): Int =
        Bukkit.getServer().profile.read(dynamicSellsKey)

    fun hasDynamicActivity(): Boolean =
        getDynamicGlobalBuys() > 0 || getDynamicGlobalSells() > 0

    fun resetDynamicPricing() {
        Bukkit.getServer().profile.write(dynamicBuysKey, 0)
        Bukkit.getServer().profile.write(dynamicSellsKey, 0)
        Bukkit.getServer().profile.write(lastDecayTimeKey, 0)
    }

    fun applyDecay() {
        val dp = dynamicPricing ?: return
        if (!dp.decayEnabled || dp.decayRate <= 0.0) return

        val nowSeconds = (System.currentTimeMillis() / 1000L).toInt()
        val lastDecay = Bukkit.getServer().profile.read(lastDecayTimeKey)

        if (lastDecay == 0) {
            Bukkit.getServer().profile.write(lastDecayTimeKey, nowSeconds)
            return
        }

        if (nowSeconds - lastDecay < dp.decayPeriodMinutes * 60) return

        val oldBuys = getDynamicGlobalBuys()
        val oldSells = getDynamicGlobalSells()
        val newBuys = (oldBuys * (1.0 - dp.decayRate)).toInt()
        val newSells = (oldSells * (1.0 - dp.decayRate)).toInt()

        Bukkit.getServer().profile.write(dynamicBuysKey, newBuys)
        Bukkit.getServer().profile.write(dynamicSellsKey, newSells)
        Bukkit.getServer().profile.write(lastDecayTimeKey, nowSeconds)

    }

    /** Get the max amount of times this player can sell this item again. */
    fun getSellsLeft(player: OfflinePlayer): Int {
        return sellLimit - getTotalSells(player)
    }

    /** Get the total amount of times a player has sold this item. */
    fun getTotalSells(player: OfflinePlayer): Int {
        return player.profile.read(timesSoldKey)
    }

    /** Get the total amount of times a server has sold this item. */
    fun getTotalGlobalSells(): Int {
        return Bukkit.getServer().profile.read(timesSoldKey)
    }

    /** If a [player] is allowed to purchase this item. */
    fun getBuyStatus(player: Player, amount: Int, buyType: BuyType): BuyStatus {
        when (buyType) {
            BuyType.NORMAL -> buyPrice ?: return BuyStatus.CANNOT_BUY
            BuyType.ALT -> altBuyPrice ?: return BuyStatus.CANNOT_BUY
        }

        if (player.profile.read(timesBoughtKey) + amount > limit) {
            return BuyStatus.BOUGHT_TOO_MANY
        }

        if (Bukkit.getServer().profile.read(timesBoughtKey) + amount > globalLimit) {
            return BuyStatus.GLOBAL_BOUGHT_TOO_MANY
        }

        if (!player.hasPermission("ecoshop.buy.$id")) {
            return BuyStatus.NO_PERMISSION
        }

        val dynamicMultiplier = getEffectiveBuyMultiplier(buyType, player)
        val conditions = if (buyType == BuyType.ALT) altBuyConditions else buyConditions
        if (!conditions.areMet(player.toDispatcher(), EmptyProvidedHolder)) {
            // Only run not met effects if player can afford to buy
            if (getBuyPrice(buyType)!!.canAfford(player, amount.toDouble() * dynamicMultiplier)) {
                conditions.areMetAndTrigger(
                    TriggerData(
                        player = player
                    ).dispatch(
                        player.toDispatcher()
                    )
                )
            }

            return BuyStatus.MISSING_REQUIREMENTS
        }

        if (!getBuyPrice(buyType)!!.canAfford(player, amount.toDouble() * dynamicMultiplier)) {
            return BuyStatus.CANNOT_AFFORD
        }

        return BuyStatus.ALLOW
    }

    /**
     * Make a [player] buy this item a certain [amount] of times.
     *
     * This handles payment and dispatching the items.
     */
    fun buy(
        player: Player,
        amount: Int,
        buyType: BuyType,
        shop: Shop? = null,
        bypassLimits: Boolean = false,
        priceValueOverride: Double? = null
    ) {
        require(if (bypassLimits) amount >= 1 else amount in 1..getMaxBuysAtOnce(player))

        val basePrice = when (buyType) {
            BuyType.NORMAL -> buyPrice
            BuyType.ALT -> altBuyPrice
        }!!

        val event = EcoShopBuyEvent(player, this, basePrice.price, buyType)
        Bukkit.getPluginManager().callEvent(event)

        val payAmount = if (priceValueOverride != null) {
            val baseValue = basePrice.getValue(player)
            if (baseValue > 0) amount.toDouble() * (priceValueOverride / baseValue) else 0.0
        } else {
            amount.toDouble() * getEffectiveBuyMultiplier(buyType, player)
        }
        event.price.pay(player, payAmount)

        if (item != null) {
            val queue = DropQueue(player)
                .forceTelekinesis()

            repeat(amount) {
                queue.addItem(item.item)
            }

            queue.push()
        }

        buyEffects?.trigger(
            DispatchedTrigger(
                player.toDispatcher(),
                TriggerBlank,
                TriggerData(
                    player = player,
                    location = player.location,
                    item = player.inventory.itemInMainHand,
                    value = amount.toDouble(),
                    altValue = basePrice.getValue(player) * amount
                )
            ).apply {
                addPlaceholder(NamedValue("amount", amount))
            }
        )

        player.profile.write(timesBoughtKey, getTotalBuys(player) + amount)
        Bukkit.getServer().profile.write(timesBoughtKey, getTotalGlobalBuys() + amount)
        Bukkit.getServer().profile.write(dynamicBuysKey, getDynamicGlobalBuys() + amount)

        if (shop?.isBroadcasting == true) {
            shop.broadcastPurchase(player, this, amount)
        }

        shop?.buySound?.playTo(player)
    }

    /** Get if a [player] is allowed to sell this item. */
    @JvmOverloads
    fun getSellStatus(player: Player, amount: Int = 1): SellStatus {
        // Can't sell a command or an effect
        if (item == null || sellPrice == null) {
            return SellStatus.CANNOT_SELL
        }

        if (getTotalSells(player) + amount > sellLimit) {
            return SellStatus.SOLD_TOO_MANY
        }

        if (getTotalGlobalSells() + amount > globalSellLimit) {
            return SellStatus.GLOBAL_SOLD_TOO_MANY
        }

        if (!player.hasPermission("ecoshop.sell.$id")) {
            return SellStatus.NO_PERMISSION
        }

        if (!sellConditions.areMet(player.toDispatcher(), EmptyProvidedHolder)) {
            return SellStatus.MISSING_REQUIREMENTS
        }

        return SellStatus.ALLOW
    }

    /** Get if a [player] is allowed to sell this item. */
    @JvmOverloads
    fun getCurrentSellStatus(player: Player, amount: Int? = null): SellStatus {
        val requestedAmount = amount ?: 1
        val base = getSellStatus(player, requestedAmount)

        if (base != SellStatus.ALLOW) {
            return base
        } else {
            val amountInInventory = getAmountInPlayerInventory(player)

            if (amountInInventory == 0) {
                return SellStatus.DONT_HAVE_ITEM
            }

            if (amount != null) {
                if (amountInInventory < amount) {
                    return SellStatus.DONT_HAVE_ENOUGH
                }
            }

            return SellStatus.ALLOW
        }
    }

    /**
     * Make a [player] sell the item up to a certain [amount] of times.
     *
     * Returns the actual amount of times the item was sold, for example if a
     * player doesn't have a certain amount of items it will sell as many as
     * possible.
     */
    fun sell(
        player: Player,
        amount: Int,
        shop: Shop? = null
    ): Int {
        if (sellPrice == null) {
            return 0
        }

        if (item == null) {
            return 0
        }

        if (getSellStatus(player) != SellStatus.ALLOW) {
            return 0
        }

        val amountSold = amount
            .coerceAtMost(getAmountInPlayerInventory(player))
            .coerceAtMost(getSellsLeft(player))
            .coerceAtMost(globalSellLimit - getTotalGlobalSells())

        if (amountSold <= 0) {
            return 0
        }

        val priceMultipliers = deductItems(player, amountSold)

        val dynamicSellMultiplier = getEffectiveSellMultiplier(player)
        for ((multiplier, times) in priceMultipliers) {
            sellPrice.giveTo(player, multiplier * dynamicSellMultiplier * times)
        }

        shop?.sellSound?.playTo(player)

        sellEffects?.trigger(
            DispatchedTrigger(
                player.toDispatcher(),
                TriggerBlank,
                TriggerData(
                    player = player,
                    location = player.location,
                    item = player.inventory.itemInMainHand,
                    value = amountSold.toDouble(),
                    altValue = sellPrice.getValue(player) * amountSold
                )
            ).apply {
                addPlaceholder(NamedValue("amount", amountSold))
            }
        )

        recordSell(player, amountSold)

        return amountSold
    }

    fun getAmountInPlayerInventory(player: Player): Int {
        if (item == null) {
            return 0
        }

        var amountOfItems = 0

        for (itemStack in player.inventory.storageContents) {
            if (itemStack == null) {
                continue
            }
            val matches = if (isStrictMatch) itemStack.isSimilar(item.item) else item.matches(itemStack)
            if (!matches) {
                continue
            }

            amountOfItems += itemStack.amount
        }

        return amountOfItems
    }

    /**
     * Deducts items, calls events, and returns a map of how many times each
     * multiplier is given.
     *
     * Map maps multipliers to amounts of times.
     */
    private fun deductItems(player: Player, amount: Int): Map<Double, Int> {
        val multipliers = mutableMapOf<Double, Int>()

        var left = amount

        if (item == null) {
            return emptyMap()
        }

        // Using slots because of some freakish bug that prevented clearing the item?
        for (i in 0..35) {
            val itemStack = player.inventory.getItem(i) ?: continue
            val matches = if (isStrictMatch) itemStack.isSimilar(item.item) else item.matches(itemStack)
            if (!matches) {
                continue
            }


            var times = 0

            if (itemStack.amount <= left) {
                left -= itemStack.amount
                times += itemStack.amount
                player.inventory.clear(i)
            } else {
                itemStack.amount -= left
                times += left
                left = 0
            }

            val event = EcoShopSellEvent(player, this, this.sellPrice!!, itemStack)
            Bukkit.getPluginManager().callEvent(event)

            multipliers[event.multiplier] = (multipliers[event.multiplier] ?: 0) + times

            if (left == 0) {
                break
            }
        }

        return multipliers
    }

    fun resetTimesBought(player: OfflinePlayer) {
        val totalBuysForPlayer = getTotalBuys(player)
        Bukkit.getServer().profile.write(timesBoughtKey, getTotalGlobalBuys() - totalBuysForPlayer)
        player.profile.write(timesBoughtKey, 0)
    }

    fun resetTimesSold(player: OfflinePlayer) {
        val totalSellsForPlayer = getTotalSells(player)
        Bukkit.getServer().profile.write(timesSoldKey, getTotalGlobalSells() - totalSellsForPlayer)
        player.profile.write(timesSoldKey, 0)
    }

    fun recordSell(player: OfflinePlayer, amount: Int) {
        if (amount <= 0) {
            return
        }

        player.profile.write(timesSoldKey, getTotalSells(player) + amount)
        Bukkit.getServer().profile.write(timesSoldKey, getTotalGlobalSells() + amount)
        Bukkit.getServer().profile.write(dynamicSellsKey, getDynamicGlobalSells() + amount)
    }

    fun getBuyPrice(buyType: BuyType) = when (buyType) {
        BuyType.ALT -> altBuyPrice
        else -> buyPrice
    }

    fun getBuyMenu(buyType: BuyType) = buyMenus[buyType]!!
}

fun ConfiguredPrice?.getDisplay(player: Player, amount: Number): String =
    this?.getDisplay(player, amount.toDouble()) ?: ""

private val itemCache = EcoCache.builder<HashedItem, Optional<ShopItem>>()
    .expireAfterAccess(Duration.ofSeconds(5))
    .build()

val ItemStack.shopItem: ShopItem?
    get() = itemCache.get(HashedItem.of(this)) {
        for (item in ShopItems.values()) {
            if (item.item?.matches(this) == true) {
                return@get Optional.of(item)
            }
        }

        Optional.ofNullable(null)
    }.orElse(null)

fun ItemStack.isSellable(player: Player): Boolean {
    val item = this.shopItem ?: return false
    if (item.getCurrentSellStatus(player, this.amount) != SellStatus.ALLOW) {
        return false
    }

    return item.sellPrice != null
}

fun ItemStack.getUnitSellValue(player: Player): ConfiguredPrice {
    val item = this.shopItem ?: return ConfiguredPrice.FREE
    if (item.getCurrentSellStatus(player, this.amount) != SellStatus.ALLOW) {
        return ConfiguredPrice.FREE
    }

    return item.sellPrice ?: ConfiguredPrice.FREE
}

/** Sell the item as a [player] and return if successful. */
fun ItemStack.sell(
    player: Player,
    shop: Shop? = null
): Boolean {
    val item = this.shopItem ?: return false
    if (item.getCurrentSellStatus(player, this.amount) != SellStatus.ALLOW) {
        return false
    }

    val price = item.sellPrice ?: return false
    val soldAmount = this.amount

    val event = EcoShopSellEvent(player, item, price, this)
    Bukkit.getPluginManager().callEvent(event)

    val dynamicSellMultiplier = item.getEffectiveSellMultiplier(player)
    price.giveTo(player, soldAmount.toDouble() * event.multiplier * dynamicSellMultiplier)
    item.recordSell(player, soldAmount)

    player.sendMessage(
        plugin.langYml.getMessage("sold-item")
            .replace("%amount%", soldAmount.toString())
            .replace("%item%", item.displayName)
            .replace("%price%", price.getDisplay(player, soldAmount.toDouble() * event.multiplier * dynamicSellMultiplier))
    )

    shop?.sellSound?.playTo(player)

    this.amount = 0
    this.type = Material.AIR

    return true
}

fun Collection<String>.formatMultiple(): String {

    return if (plugin.langYml.has("multiple-format.${this.size}")) {
        var base = plugin.langYml.getString("multiple-format.${this.size}")
        for ((i, element) in this.withIndex()) {
            base = base.replace($$"$$$i", element)
        }

        base
    } else {
        this.joinToString("&r&f, &r")
    }
}

/**
 * Sell a group of items as a [player] and return all the unsold items.
 */
fun Collection<ItemStack>.sell(
    player: Player,
    shop: Shop? = null
): Collection<ItemStack> {
    val unsold = mutableListOf<ItemStack>()
    var amountSold = 0
    val displayBuilder = CombinedDisplayPrice.builder(player)

    for (itemStack in this) {
        val item = itemStack.shopItem
        if (item == null) {
            unsold += itemStack
            continue
        }

        if (item.getSellStatus(player, itemStack.amount) != SellStatus.ALLOW) {
            unsold += itemStack
            continue
        }

        val sellableAmount = itemStack.amount
            .coerceAtMost(item.getSellsLeft(player))
            .coerceAtMost(item.globalSellLimit - item.getTotalGlobalSells())

        if (sellableAmount <= 0) {
            unsold += itemStack
            continue
        }

        val price = item.sellPrice!!

        val event = EcoShopSellEvent(player, item, price, itemStack)
        Bukkit.getPluginManager().callEvent(event)

        val dynamicSellMultiplier = item.getEffectiveSellMultiplier(player)
        price.giveTo(player, sellableAmount.toDouble() * event.multiplier * dynamicSellMultiplier)
        item.recordSell(player, sellableAmount)

        displayBuilder.add(
            price,
            sellableAmount.toDouble() * event.multiplier * dynamicSellMultiplier
        )

        amountSold += sellableAmount

        if (sellableAmount >= itemStack.amount) {
            itemStack.amount = 0
            itemStack.type = Material.AIR
        } else {
            itemStack.amount -= sellableAmount
            unsold += itemStack
        }
    }

    // If none sold.
    if (amountSold == 0) {
        return unsold
    }

    shop?.sellSound?.playTo(player)

    player.sendMessage(
        plugin.langYml.getMessage("sold-multiple")
            .replace("%amount%", amountSold.toString())
            .replace("%price%", displayBuilder.build().displayStrings.toList().formatMultiple().formatEco(player))
    )

    return unsold
}