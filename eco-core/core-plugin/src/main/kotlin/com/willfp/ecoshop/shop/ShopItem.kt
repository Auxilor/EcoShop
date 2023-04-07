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
import com.willfp.eco.util.formatEco
import com.willfp.ecoshop.EcoShopPlugin
import com.willfp.ecoshop.event.EcoShopSellEvent
import com.willfp.ecoshop.shop.gui.BuyMenu
import com.willfp.ecoshop.shop.gui.SellMenu
import com.willfp.ecoshop.shop.gui.ShopItemSlot
import org.bukkit.Bukkit
import org.bukkit.ChatColor
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
    plugin: EcoShopPlugin,
    val config: Config
) {
    val id = config.getString("id")

    val commands = config.getStringsOrNull("command")

    val sellCommand = config.getStringsOrNull("sell.sell-commands")

    val sellItemMessage = config.getStringsOrNull("sell.sell-message")

    val buyItemMessage = config.getStringsOrNull("buy.buy-message")

    val item = if (config.has("item")) Items.lookup(config.getString("item")) else null

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

    val slot = ShopItemSlot(this, plugin)

    val isShowingQuickBuySell = config.getBoolOrNull("gui.show-quick-buy-sell") ?: true

    private val buyMenus = BuyType.values().associateWith { BuyMenu(this, plugin, it) }

    val sellMenu = SellMenu(this, plugin)

    private val limit = config.getIntOrNull("buy.limit") ?: Int.MAX_VALUE

    private val maxAtOnce = config.getIntOrNull("buy.max-at-once") ?: Int.MAX_VALUE

    private val timesBoughtKey = PersistentDataKey(
        plugin.createNamespacedKey("${id}_times_bought"),
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
    fun getBuysLeft(player: Player): Int {
        return limit - player.profile.read(timesBoughtKey)
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

        if (!player.hasPermission("ecoshop.buy.$id")) {
            return BuyStatus.NO_PERMISSION
        }

        if (config.has("buy.require")) {
            if (config.getDoubleFromExpression("buy.require", player) != 1.0) {
                return BuyStatus.MISSING_REQUIREMENTS
            }
        }

        if (!getBuyPrice(buyType)!!.canAfford(player, amount.toDouble())) {
            return BuyStatus.CANNOT_AFFORD
        }

        return BuyStatus.ALLOW
    }

    /**
     * Make a [player] buy this item a certain [amount] of times.
     *
     * This handles payment and dispatching the items / commands.
     */
    fun buy(
        player: Player,
        amount: Int,
        buyType: BuyType,
        shop: Shop? = null
    ) {
        require(amount in 1..getMaxBuysAtOnce(player))

        when (buyType) {
            BuyType.NORMAL -> buyPrice?.pay(player, amount.toDouble())
            BuyType.ALT -> altBuyPrice?.pay(player, amount.toDouble())
        }

        if (item != null) {
            val queue = DropQueue(player)
                .forceTelekinesis()

            repeat(amount) {
                queue.addItem(item.item)
            }

            queue.push()
        }

        if (commands != null) {
            for (command in commands) {
                Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    command.replace("%player%", player.name)
                        .replace("%amount%", amount.toString())
                )
            }
        }
        if (buyItemMessage != null) {
            for (message in buyItemMessage) {
                player.sendMessage(
                    message.formatEco()
                        .replace("%player%", player.name)
                        .replace("%amount%", amount.toString())
                )
            }
        }

        player.profile.write(timesBoughtKey, player.profile.read(timesBoughtKey) + 1)

        if (shop?.isBroadcasting == true) {
            shop.broadcastPurchase(player, this, amount)
        }

        shop?.buySound?.playTo(player)
    }

    /** Get if a [player] is allowed to sell this item. */
    fun getSellStatus(player: Player): SellStatus {
        // Can't sell a command
        if (item == null || sellPrice == null) {
            return SellStatus.CANNOT_SELL
        }

        if (!player.hasPermission("ecoshop.sell.$id")) {
            return SellStatus.NO_PERMISSION
        }

        if (config.has("sell.require")) {
            if (config.getDoubleFromExpression("sell.require", player) != 1.0) {
                return SellStatus.MISSING_REQUIREMENTS
            }
        }

        return SellStatus.ALLOW
    }

    /** Get if a [player] is allowed to sell this item. */
    @JvmOverloads
    fun getCurrentSellStatus(player: Player, amount: Int? = null): SellStatus {
        val base = getSellStatus(player)

        if (base != SellStatus.ALLOW) {
            return base
        } else {
            if (getAmountInPlayerInventory(player) == 0) {
                return SellStatus.DONT_HAVE_ITEM
            }

            if (amount != null) {
                if (getAmountInPlayerInventory(player) < amount) {
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

        val amountSold = amount.coerceAtMost(getAmountInPlayerInventory(player))

        val priceMultipliers = deductItems(player, amountSold)

        for ((multiplier, times) in priceMultipliers) {
            sellPrice.giveTo(player, multiplier * times)
        }

        shop?.sellSound?.playTo(player)

        if (sellCommand != null) {
            for (command in sellCommand) {
                Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    command.replace("%player%", player.name)
                        .replace("%amount%", amountSold.toString())
                )
            }
        }
        if (sellItemMessage != null) {
            for (message in sellItemMessage) {
                player.sendMessage(
                    message.formatEco()
                        .replace("%player%", player.name)
                        .replace("%amount%", amountSold.toString())
                )
            }
        }

        return amountSold
        }

    fun getAmountInPlayerInventory(player: Player): Int {
        if (item == null) {
            return 0
        }

        var amountOfItems = 0

        for (itemStack in player.inventory.storageContents) {
            if (!item.matches(itemStack)) {
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
            val itemStack = player.inventory.getItem(i)

            if (!item.matches(itemStack) || itemStack == null) {
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
        player.profile.write(timesBoughtKey, 0)
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
    if (item.getSellStatus(player) != SellStatus.ALLOW) {
        return false
    }

    return item.sellPrice != null
}

fun ItemStack.getUnitSellValue(player: Player): ConfiguredPrice {
    val item = this.shopItem ?: return ConfiguredPrice.FREE
    if (item.getSellStatus(player) != SellStatus.ALLOW) {
        return ConfiguredPrice.FREE
    }

    return item.sellPrice ?: ConfiguredPrice.FREE
}

/** Sell the item as a [player] and return if successful. */
fun ItemStack.sell(
    player: Player,
    shop: Shop? = null
): Boolean {
    if (!this.isSellable(player)) {
        return false
    }

    val price = this.getUnitSellValue(player)
    val item = this.shopItem!!

    val event = EcoShopSellEvent(player, item, item.sellPrice!!, this)
    Bukkit.getPluginManager().callEvent(event)

    price.giveTo(player, this.amount.toDouble() * event.multiplier)

    player.sendMessage(
        EcoShopPlugin.instance.langYml.getMessage("sold-item")
            .replace("%amount%", this.amount.toString())
            .replace("%item%", item.displayName)
            .replace("%price%", price.getDisplay(player, this.amount.toDouble() * event.multiplier))
    )

    shop?.sellSound?.playTo(player)

    this.amount = 0
    this.type = Material.AIR

    return true
}

fun Collection<String>.formatMultiple(): String {
    val langYml = EcoShopPlugin.instance.langYml

    return if (langYml.has("multiple-format.${this.size}")) {
        var base = langYml.getString("multiple-format.${this.size}")
        for ((i, element) in this.withIndex()) {
            base = base.replace("\$$i", element)
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
        if (!itemStack.isSellable(player)) {
            unsold += itemStack
        }

        val price = itemStack.getUnitSellValue(player)
        val item = itemStack.shopItem!!

        val event = EcoShopSellEvent(player, item, item.sellPrice!!, itemStack)
        Bukkit.getPluginManager().callEvent(event)

        price.giveTo(player, itemStack.amount.toDouble() * event.multiplier)

        displayBuilder.add(
            price,
            itemStack.amount.toDouble() * event.multiplier
        )

        amountSold += itemStack.amount
        itemStack.amount = 0
        itemStack.type = Material.AIR
    }

    // If none sold.
    if (unsold.size == this.size) {
        return unsold
    }

    shop?.sellSound?.playTo(player)

    player.sendMessage(
        EcoShopPlugin.instance.langYml.getMessage("sold-multiple")
            .replace("%amount%", amountSold.toString())
            .replace("%price%", displayBuilder.build().displayStrings.toList().formatMultiple().formatEco(player))
    )

    return unsold
}
