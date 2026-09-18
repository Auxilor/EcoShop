package com.willfp.ecoshop.sell

import com.willfp.eco.core.price.ConfiguredPrice
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SellTotalsTest {
    private fun candidate(type: String): SellCandidate = testCandidate(type).also {
        val price = mockk<ConfiguredPrice> { every { identifier } returns type }
        every { it.sellPrice } returns price
    }

    @Test
    fun `plus merges units and per-type values`() {
        val a = SellTotals(3, mapOf("eco:economy" to 10.0))
        val b = SellTotals(2, mapOf("eco:economy" to 5.0, "eco:item-1" to 4.0))
        assertEquals(SellTotals(5, mapOf("eco:economy" to 15.0, "eco:item-1" to 4.0)), a + b)
    }

    @Test
    fun `encode and decode round trip`() {
        val totals = SellTotals(1234, mapOf("eco:economy" to 99.5, "weird;type=x" to 2.0))
        assertEquals(totals, SellTotals.decode(totals.encode()))
    }

    @Test
    fun `decode tolerates bad input`() {
        assertTrue(SellTotals.decode(null).isEmpty)
        assertTrue(SellTotals.decode("").isEmpty)
        assertTrue(SellTotals.decode("nonsense").isEmpty)
        assertEquals(SellTotals(4, mapOf("eco:economy" to 1.0)), SellTotals.decode("4;eco%3Aeconomy=1.0;broken;x=y"))
    }

    @Test
    fun `of result groups paid lines by price type`() {
        val money = candidate("eco:economy")
        val gems = candidate("eco:item-7")
        val result = SellResult(
            listOf(
                PaidLine(money, 5, 5.0, 50.0, 50.0),
                PaidLine(gems, 2, 2.0, null, 6.0),
                PaidLine(money, 1, 1.0, 10.0, 10.0)
            ),
            emptyList(), emptyList()
        )
        assertEquals(SellTotals(8, mapOf("eco:economy" to 60.0, "eco:item-7" to 6.0)), SellTotals.of(result))
    }

    @Test
    fun `of quote uses base times price multiplier`() {
        val money = candidate("eco:economy")
        val request = mockk<SellRequest>()
        val quote = SellQuote(request, listOf(QuoteLine(money, emptyList(), 4, 10.0, 4.0, 40.0)), emptyList())
        assertEquals(SellTotals(4, mapOf("eco:economy" to 40.0)), SellTotals.of(quote))
    }

    @Test
    fun `render values uses per type format with fallback`() {
        val totals = SellTotals(3, mapOf("eco:economy" to 1500.0, "eco:item-7" to 6.0))
        val formats = mapOf("eco:economy" to "$%value_commas%")
        val rendered = renderValues(totals) { formats[it] }

        assertEquals(2, rendered.size)
        assertTrue(rendered[0].startsWith("$1,500"), rendered[0])
        assertTrue(rendered[1].startsWith("6"), rendered[1])
        assertFalse(rendered[1].contains("$"))
    }
}
