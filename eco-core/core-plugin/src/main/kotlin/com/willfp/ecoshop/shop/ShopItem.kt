@file:JvmName("ShopItemUtils")

package com.willfp.ecoshop.shop

import com.github.benmanes.caffeine.cache.Caffeine
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
import com.willfp.eco.util.formatEco
import com.willfp.ecoshop.event.EcoShopBuyEvent
import com.willfp.ecoshop.event.EcoShopSellEvent
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.shop.gui.BuyMenu
import com.willfp.ecoshop.shop.gui.SellMenu
import com.willfp.ecoshop.shop.gui.ShopItemSlot
import com.willfp.libreforge.EmptyProvidedHolder
import com.willfp.libreforge.ViolationContext
import com.willfp.libreforge.conditions.Conditions
import com.willfp.libreforge.effects.Effects
import com.willfp.libreforge.toDispatcher
import com.willfp.libreforge.triggers.TriggerData
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import java.util.Optional
import java.util.concurrent.TimeUnit

enum class BuyType {
    NORMAL,
    ALT
}

class ShopItem(
    val config: Config
) : KRegistrable {
    override val id = config.getString("id")

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
        } else item?.item ?: ItemStack(Material.AIR)
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

        val conditions = if (buyType == BuyType.ALT) altBuyConditions else buyConditions
        if (!conditions.areMet(player.toDispatcher(), EmptyProvidedHolder)) {
            // Only run not met effects if player can afford to buy
            if (getBuyPrice(buyType)!!.canAfford(player, amount.toDouble())) {
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

        if (!getBuyPrice(buyType)!!.canAfford(player, amount.toDouble())) {
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
        shop: Shop? = null
    ) {
        require(amount in 1..getMaxBuysAtOnce(player))

        val basePrice = when (buyType) {
            BuyType.NORMAL -> buyPrice
            BuyType.ALT -> altBuyPrice
        }!!

        val event = EcoShopBuyEvent(player, this, basePrice.price, buyType)
        Bukkit.getPluginManager().callEvent(event)

        event.price.pay(player, amount.toDouble())

        if (item != null) {
            val queue = DropQueue(player)
                .forceTelekinesis()

            repeat(amount) {
                queue.addItem(item.item)
            }

            queue.push()
        }

        buyEffects?.trigger(
            player.toDispatcher(),
            TriggerData(
                player = player,
                location = player.location,
                item = player.inventory.itemInMainHand,
                value = amount.toDouble(),
                altValue = basePrice.getValue(player) * amount
            )
        )

        player.profile.write(timesBoughtKey, getTotalBuys(player) + 1)
        Bukkit.getServer().profile.write(timesBoughtKey, getTotalGlobalBuys() + 1)

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

        for ((multiplier, times) in priceMultipliers) {
            sellPrice.giveTo(player, multiplier * times)
        }

        shop?.sellSound?.playTo(player)

        sellEffects?.trigger(
            player.toDispatcher(),
            TriggerData(
                player = player,
                location = player.location,
                item = player.inventory.itemInMainHand,
                value = amountSold.toDouble(),
                altValue = sellPrice.getValue(player) * amountSold
            )
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
    }

    fun getBuyPrice(buyType: BuyType) = when (buyType) {
        BuyType.ALT -> altBuyPrice
        else -> buyPrice
    }

    fun getBuyMenu(buyType: BuyType) = buyMenus[buyType]!!
}

fun ConfiguredPrice?.getDisplay(player: Player, amount: Number): String =
    this?.getDisplay(player, amount.toDouble()) ?: ""

private val itemCache = Caffeine.newBuilder()
    .expireAfterAccess(5, TimeUnit.SECONDS)
    .build<HashedItem, Optional<ShopItem>>()

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

    price.giveTo(player, soldAmount.toDouble() * event.multiplier)
    item.recordSell(player, soldAmount)

    player.sendMessage(
        plugin.langYml.getMessage("sold-item")
            .replace("%amount%", soldAmount.toString())
            .replace("%item%", item.displayName)
            .replace("%price%", price.getDisplay(player, soldAmount.toDouble() * event.multiplier))
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

        price.giveTo(player, sellableAmount.toDouble() * event.multiplier)
        item.recordSell(player, sellableAmount)

        displayBuilder.add(
            price,
            sellableAmount.toDouble() * event.multiplier
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