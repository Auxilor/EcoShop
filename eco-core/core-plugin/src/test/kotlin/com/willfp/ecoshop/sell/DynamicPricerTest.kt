package com.willfp.ecoshop.sell

import com.willfp.ecoshop.shop.PriceDynamicConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DynamicPricerTest {
    private val warnings = mutableListOf<String>()

    @BeforeEach
    fun setUp() {
        // Tiny evaluator: supports "a * b" and "a - b" and plain numbers, enough for tests.
        DynamicPricer.evaluator = { expr -> evalSimple(expr) }
        DynamicPricer.warn = { warnings += it }
        DynamicPricer.resetWarnings()
    }

    @AfterEach
    fun tearDown() = DynamicPricer.resetDefaults()

    @Test
    fun `disabled config returns base`() {
        val cfg = PriceDynamicConfig(false, 2.0, 0.5, "%base_price% * 2")
        assertEquals(10.0, DynamicPricer.sellValue(cfg, 10.0, 0, 100, "x"))
    }

    @Test
    fun `null config returns base and factor one`() {
        assertEquals(10.0, DynamicPricer.sellValue(null, 10.0, 0, 0, "x"))
        assertEquals(1.0, DynamicPricer.sellFactor(null, 10.0, 0, 0, "x"))
    }

    @Test
    fun `formula substitutes counters`() {
        val cfg = PriceDynamicConfig(true, 2.0, 0.1, "%base_price% - %sells%")
        assertEquals(7.0, DynamicPricer.sellValue(cfg, 10.0, 0, 3, "x"))
    }

    @Test
    fun `result clamps to max decrease`() {
        val cfg = PriceDynamicConfig(true, 2.0, 0.5, "%base_price% - %sells%")
        assertEquals(5.0, DynamicPricer.sellValue(cfg, 10.0, 0, 9, "x"))
    }

    @Test
    fun `result rounds to two decimals`() {
        val cfg = PriceDynamicConfig(true, 2.0, 0.0, "%base_price% * 0.33333")
        assertEquals(3.33, DynamicPricer.sellValue(cfg, 10.0, 0, 0, "x"))
    }

    @Test
    fun `zero result with nonzero base falls back and warns once`() {
        val cfg = PriceDynamicConfig(true, 2.0, 0.0, "%base_price% * 0")
        assertEquals(10.0, DynamicPricer.sellValue(cfg, 10.0, 0, 0, "item"))
        assertEquals(10.0, DynamicPricer.sellValue(cfg, 10.0, 0, 0, "item"))
        assertEquals(1, warnings.size)
    }

    @Test
    fun `factor is value over base`() {
        val cfg = PriceDynamicConfig(true, 2.0, 0.1, "%base_price% - %sells%")
        assertEquals(0.8, DynamicPricer.sellFactor(cfg, 10.0, 0, 2, "x"), 1e-9)
        assertEquals(1.0, DynamicPricer.sellFactor(cfg, 0.0, 0, 2, "x"))
    }

    private fun evalSimple(expr: String): Double {
        val e = expr.trim()
        e.toDoubleOrNull()?.let { return it }
        if (" * " in e) return e.split(" * ").map { it.toDouble() }.reduce(Double::times)
        if (" - " in e) return e.split(" - ").map { it.toDouble() }.reduce(Double::minus)
        return Double.NaN
    }
}
