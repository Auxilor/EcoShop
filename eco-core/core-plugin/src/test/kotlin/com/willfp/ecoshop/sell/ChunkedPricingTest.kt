package com.willfp.ecoshop.sell

import com.willfp.ecoshop.fakeStack
import com.willfp.ecoshop.shop.PriceDynamicConfig
import io.mockk.mockk
import org.bukkit.entity.Player
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ChunkedPricingTest {
    // base - sells, clamped to [0, 2x base]
    private val dyn = PriceDynamicConfig(true, 2.0, 0.0, "%base_price% - %sells%")
    private val seller = Seller.Online(mockk<Player>(relaxed = true))

    @BeforeEach
    fun setUp() {
        DynamicPricer.evaluator = { expr ->
            val (a, b) = expr.split(" - ").map { it.toDouble() }
            a - b
        }
        DynamicPricer.warn = {}
    }

    @AfterEach
    fun tearDown() = DynamicPricer.resetDefaults()

    private fun multiplierFor(chunk: Int, units: Int, startSells: Int = 0): Double {
        val c = testCandidate("diamond", base = 100.0, dynamic = dyn, dynamicSells = startSells)
        val s = fakeStack(units)
        val q = SellQuoter(resolve = { c }, chunkSize = { chunk })
            .quote(SellRequest(seller, SellSource.COMMAND, ListTarget(listOf(s))))
        return q.lines.single().priceMultiplier
    }

    @Test
    fun `chunk size one integrates per unit`() {
        assertEquals(1.0 + 0.99 + 0.98 + 0.97, multiplierFor(chunk = 1, units = 4), 1e-9)
    }

    @Test
    fun `chunk size two reprices between chunks`() {
        assertEquals(2 * 1.0 + 2 * 0.98, multiplierFor(chunk = 2, units = 4), 1e-9)
    }

    @Test
    fun `chunk at least units matches old single read`() {
        assertEquals(4.0, multiplierFor(chunk = 64, units = 4), 1e-9)
    }

    @Test
    fun `starting counter is respected`() {
        assertEquals(2 * 0.9, multiplierFor(chunk = 64, units = 2, startSells = 10), 1e-9)
    }

    @Test
    fun `bypass dynamic pricing pays flat base`() {
        val c = testCandidate("diamond", base = 100.0, dynamic = dyn, dynamicSells = 10)
        val s = fakeStack(4)
        val q = SellQuoter(resolve = { c }, chunkSize = { 1 })
            .quote(SellRequest(seller, SellSource.COMMAND, ListTarget(listOf(s)), bypass = SellBypass(dynamicPricing = true)))
        assertEquals(4.0, q.lines.single().priceMultiplier, 1e-9)
    }

    @Test
    fun `chunk size below one is treated as one`() {
        assertEquals(multiplierFor(chunk = 1, units = 3), multiplierFor(chunk = 0, units = 3), 1e-9)
    }
}
