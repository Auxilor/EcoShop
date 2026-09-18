package com.willfp.ecoshop

import io.mockk.every
import io.mockk.mockk
import org.bukkit.inventory.ItemStack

/** A mocked ItemStack whose amount can be read and written without a server. */
fun fakeStack(amount: Int): ItemStack {
    var current = amount
    val stack = mockk<ItemStack>(relaxed = true)
    every { stack.amount } answers { current }
    every { stack.amount = any() } answers { current = firstArg() }
    every { stack.isEmpty } answers { current <= 0 }
    return stack
}
