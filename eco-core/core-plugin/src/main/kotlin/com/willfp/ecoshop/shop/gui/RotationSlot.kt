package com.willfp.ecoshop.shop.gui

import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.slot.Slot
import com.willfp.ecoshop.shop.ShopItems
import com.willfp.ecoshop.shop.rotation.CategoryRotationState
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class RotationSlot(
    private val slotIndex: Int,
    private val getState: () -> CategoryRotationState
) : Slot {
    // Explicit AIR so getItemStack never returns null for an empty slot.
    private val empty = Slot.builder(ItemStack(Material.AIR)).build()

    private fun currentSlot(): Slot {
        val itemId = getState().activeItems.getOrNull(slotIndex)
            ?.takeIf { it.isNotEmpty() }
            ?: return empty
        return ShopItems[itemId]?.slot ?: empty
    }

    override fun getItemStack(player: Player): ItemStack =
        currentSlot().getItemStack(player)

    override fun getActionableSlot(player: Player, menu: Menu): Slot =
        currentSlot().getActionableSlot(player, menu)
}
