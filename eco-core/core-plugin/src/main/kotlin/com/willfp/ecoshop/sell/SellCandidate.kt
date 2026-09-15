package com.willfp.ecoshop.sell

import com.willfp.eco.core.price.ConfiguredPrice
import com.willfp.ecoshop.shop.PriceDynamicConfig
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/** Everything the sell core needs from a shop item. Implemented by ShopItem. */
interface SellCandidate {
    val id: String
    val categoryIds: Set<String>
    val sellPrice: ConfiguredPrice?
    val rawSellValueExpression: String?
    val hasSellConditions: Boolean
    val sellLimit: Int
    val globalSellLimit: Int
    val sellDynamicConfig: PriceDynamicConfig?
    val isSellable: Boolean

    fun matchesForSale(itemStack: ItemStack): Boolean
    fun totalSells(player: OfflinePlayer): Int
    fun totalGlobalSells(): Int
    fun dynamicGlobalBuys(): Int
    fun dynamicGlobalSells(): Int
    fun hasSellPermission(player: Player): Boolean
    fun sellConditionsMet(player: Player): Boolean

    /** Base unit value. [player] null means offline: evaluate the raw expression without placeholders. */
    fun baseSellValue(player: Player?): Double
    fun isEconomyPrice(): Boolean

    /** Fires EcoShopSellEvent and returns the resulting event multiplier. */
    fun fireSellEvent(player: Player, stack: ItemStack, units: Int, source: SellSource): Double
    fun triggerSellEffects(player: Player, units: Int)
    fun giveSellPayout(player: Player, multiplier: Double)

    /** Adds [amount] to the player, global and dynamic sell counters, skipping any counter [bypass] turns off. */
    fun recordSell(player: OfflinePlayer, amount: Int, bypass: SellBypass)
}
