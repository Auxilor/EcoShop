package com.willfp.ecoshop.sellwand

import com.willfp.eco.core.fast.fast
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.sell.SellTotals
import com.willfp.ecoshop.sell.SellValueFormat
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.Locale
import java.util.UUID

internal fun nextWandUses(current: Int): Int =
    if (current < 0) current else (current - 1).coerceAtLeast(0)

internal fun renderWandLore(
    template: List<String>,
    uses: Int,
    maxUses: Int,
    multiplier: Double,
    soldItems: Long = 0,
    soldValue: String = "0"
): List<String> {
    fun fmt(value: Int) = if (value < 0) "∞" else value.toString()
    return template.map {
        it.replace("%uses%", fmt(uses))
            .replace("%max_uses%", fmt(maxUses))
            .replace("%multiplier%", multiplier.toString())
            .replace("%sold_items%", "%,d".format(Locale.ROOT, soldItems))
            .replace("%sold_value%", soldValue)
    }
}

/** Builds wand items and reads/writes their persistent state. */
object WandItem {
    private val idKey = plugin.createNamespacedKey("sellwand_id")
    private val usesKey = plugin.createNamespacedKey("sellwand_uses")
    private val uuidKey = plugin.createNamespacedKey("sellwand_uuid")
    private val soldKey = plugin.createNamespacedKey("sellwand_sold")

    fun create(wand: SellWand): ItemStack {
        val stack = wand.baseItem.clone()
        stack.amount = 1
        val meta = stack.itemMeta
        val pdc = meta.persistentDataContainer
        pdc.set(idKey, PersistentDataType.STRING, wand.id)
        pdc.set(uuidKey, PersistentDataType.STRING, UUID.randomUUID().toString())
        if (wand.uses >= 0) {
            pdc.set(usesKey, PersistentDataType.INTEGER, wand.uses)
        }
        meta.setMaxStackSize(1)
        stack.itemMeta = meta
        stack.fast().lore = renderWandLore(wand.loreTemplate, wand.uses, wand.uses, wand.multiplier)
        return stack
    }

    fun isWand(stack: ItemStack?): Boolean = idOf(stack) != null

    /** The wand type id stored on [stack], even if that type no longer exists. */
    fun idOf(stack: ItemStack?): String? =
        stack?.itemMeta?.persistentDataContainer?.get(idKey, PersistentDataType.STRING)

    fun wandOf(stack: ItemStack?): SellWand? = SellWands[idOf(stack) ?: return null]

    /** Remaining uses, or -1 for infinite. */
    fun usesOf(stack: ItemStack): Int =
        stack.itemMeta?.persistentDataContainer?.get(usesKey, PersistentDataType.INTEGER) ?: -1

    fun totalsOf(stack: ItemStack): SellTotals =
        SellTotals.decode(stack.itemMeta?.persistentDataContainer?.get(soldKey, PersistentDataType.STRING))

    fun setUses(stack: ItemStack, wand: SellWand, uses: Int) = write(stack, wand, uses, totalsOf(stack), null)

    /** Stores remaining uses and adds [sold] to the wand's lifetime stats, then re-renders lore. */
    fun recordSale(stack: ItemStack, wand: SellWand, uses: Int, sold: SellTotals, player: Player) =
        write(stack, wand, uses, totalsOf(stack) + sold, player)

    private fun write(stack: ItemStack, wand: SellWand, uses: Int, totals: SellTotals, player: Player?) {
        val meta = stack.itemMeta
        val pdc = meta.persistentDataContainer
        if (uses >= 0) {
            pdc.set(usesKey, PersistentDataType.INTEGER, uses)
        }
        if (!totals.isEmpty) {
            pdc.set(soldKey, PersistentDataType.STRING, totals.encode())
        }
        stack.itemMeta = meta
        stack.fast().lore = renderWandLore(
            wand.loreTemplate, uses, wand.uses, wand.multiplier,
            totals.units, SellValueFormat.format(totals, player)
        )
    }
}
