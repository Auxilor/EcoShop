package com.willfp.ecoshop.sellwand

import com.willfp.eco.core.fast.fast
import com.willfp.ecoshop.plugin
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

internal fun nextWandUses(current: Int): Int =
    if (current < 0) current else (current - 1).coerceAtLeast(0)

internal fun renderWandLore(template: List<String>, uses: Int, maxUses: Int, multiplier: Double): List<String> {
    fun fmt(value: Int) = if (value < 0) "∞" else value.toString()
    return template.map {
        it.replace("%uses%", fmt(uses))
            .replace("%max_uses%", fmt(maxUses))
            .replace("%multiplier%", multiplier.toString())
    }
}

/** Builds wand items and reads/writes their persistent state. */
object WandItem {
    private val idKey = plugin.createNamespacedKey("sellwand_id")
    private val usesKey = plugin.createNamespacedKey("sellwand_uses")
    private val uuidKey = plugin.createNamespacedKey("sellwand_uuid")

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

    fun setUses(stack: ItemStack, wand: SellWand, uses: Int) {
        val meta = stack.itemMeta
        if (uses >= 0) {
            meta.persistentDataContainer.set(usesKey, PersistentDataType.INTEGER, uses)
        }
        stack.itemMeta = meta
        stack.fast().lore = renderWandLore(wand.loreTemplate, uses, wand.uses, wand.multiplier)
    }
}
