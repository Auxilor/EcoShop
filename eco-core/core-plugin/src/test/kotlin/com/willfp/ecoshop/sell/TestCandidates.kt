package com.willfp.ecoshop.sell

import com.willfp.eco.core.price.ConfiguredPrice
import com.willfp.ecoshop.shop.PriceDynamicConfig
import io.mockk.every
import io.mockk.mockk
import org.bukkit.inventory.ItemStack

/** Builds a mocked SellCandidate with sensible defaults. */
fun testCandidate(
    id: String,
    base: Double = 10.0,
    sellLimit: Int = Int.MAX_VALUE,
    globalSellLimit: Int = Int.MAX_VALUE,
    playerSells: Int = 0,
    globalSells: Int = 0,
    dynamicSells: Int = 0,
    dynamic: PriceDynamicConfig? = null,
    economy: Boolean = true,
    permitted: Boolean = true,
    conditionsMet: Boolean = true,
    hasConditions: Boolean = false,
    rawExpression: String? = base.toString(),
    matches: (ItemStack) -> Boolean = { true }
): SellCandidate = mockk(relaxed = true) {
    every { this@mockk.id } returns id
    every { categoryIds } returns emptySet()
    every { sellPrice } returns mockk<ConfiguredPrice>(relaxed = true)
    every { isSellable } returns true
    every { this@mockk.sellLimit } returns sellLimit
    every { this@mockk.globalSellLimit } returns globalSellLimit
    every { totalSells(any()) } returns playerSells
    every { totalGlobalSells() } returns globalSells
    every { dynamicGlobalBuys() } returns 0
    every { dynamicGlobalSells() } returns dynamicSells
    every { sellDynamicConfig } returns dynamic
    every { isEconomyPrice() } returns economy
    every { hasSellPermission(any()) } returns permitted
    every { sellConditionsMet(any()) } returns conditionsMet
    every { hasSellConditions } returns hasConditions
    every { rawSellValueExpression } returns rawExpression
    every { baseSellValue(any()) } returns base
    every { baseSellValue(null) } returns base
    every { matchesForSale(any()) } answers { matches(firstArg()) }
}
