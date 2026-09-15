package com.willfp.ecoshop.sell

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/** An indexed set of stacks the sell core reads from and removes from. */
interface SellTarget {
    val indices: List<Int>

    fun get(index: Int): ItemStack?

    fun remove(index: Int, amount: Int)
}

/** Backed by a live inventory (player inventory slots, container inventory). */
class InventoryTarget(
    private val inventory: Inventory,
    override val indices: List<Int> = (0 until inventory.size).toList()
) : SellTarget {
    override fun get(index: Int): ItemStack? = inventory.getItem(index)

    override fun remove(index: Int, amount: Int) {
        val stack = inventory.getItem(index) ?: return
        if (amount >= stack.amount) {
            inventory.clear(index)
        } else {
            stack.amount -= amount
            inventory.setItem(index, stack)
        }
    }
}

/** Backed by loose stacks (GUI captive items, adapter calls). Removal mutates the stacks. */
class ListTarget(private val stacks: List<ItemStack>) : SellTarget {
    override val indices: List<Int> = stacks.indices.toList()

    override fun get(index: Int): ItemStack? = stacks.getOrNull(index)

    override fun remove(index: Int, amount: Int) {
        val stack = stacks.getOrNull(index) ?: return
        stack.amount = (stack.amount - amount).coerceAtLeast(0)
    }
}
