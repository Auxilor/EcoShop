package com.willfp.ecoshop.sell

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoshop.fakeStack
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ItemFilterTest {
    private fun candidate(id: String, vararg categories: String): SellCandidate = mockk {
        every { this@mockk.id } returns id
        every { categoryIds } returns categories.toSet()
    }

    private val lookup = object : FilterLookup {
        private val shops = mapOf("main" to setOf("minerals"), "farm" to setOf("crops"))
        override fun shopCategories(shopId: String) = shops[shopId]
        override fun categoryExists(id: String) = id in setOf("minerals", "blocks", "crops")
        override fun shopItemExists(id: String) = id in setOf("diamond", "cobblestone", "wheat")
    }

    private val diamond = candidate("diamond", "minerals")
    private val cobble = candidate("cobblestone", "blocks")
    private val wheat = candidate("wheat", "crops")
    private val stack = fakeStack(1)

    @Test
    fun `none allows everything with multiplier one`() {
        assertTrue(ItemFilter.NONE.allows(diamond, stack))
        assertEquals(1.0, ItemFilter.NONE.multiplierFor(diamond, stack))
    }

    @Test
    fun `whitelist restricts to matches`() {
        val filter = ItemFilter(listOf(CategoryRule("minerals")), emptyList(), emptyList())
        assertTrue(filter.allows(diamond, stack))
        assertFalse(filter.allows(cobble, stack))
    }

    @Test
    fun `blacklist wins over whitelist`() {
        val filter = ItemFilter(listOf(CategoryRule("minerals")), listOf(ShopItemRule("diamond")), emptyList())
        assertFalse(filter.allows(diamond, stack))
    }

    @Test
    fun `shops restrict to their categories`() {
        val filter = ItemFilter(emptyList(), emptyList(), emptyList(), shops = setOf("main"), lookup = lookup)
        assertTrue(filter.allows(diamond, stack))
        assertFalse(filter.allows(cobble, stack))
    }

    @Test
    fun `shops and with whitelist`() {
        val filter = ItemFilter(
            listOf(ShopItemRule("wheat"), ShopItemRule("diamond")), emptyList(), emptyList(),
            shops = setOf("main"), lookup = lookup
        )
        assertTrue(filter.allows(diamond, stack))
        assertFalse(filter.allows(wheat, stack))
    }

    @Test
    fun `multiple shops union their categories`() {
        val filter = ItemFilter(emptyList(), emptyList(), emptyList(), shops = setOf("main", "farm"), lookup = lookup)
        assertTrue(filter.allows(diamond, stack))
        assertTrue(filter.allows(wheat, stack))
        assertFalse(filter.allows(cobble, stack))
    }

    @Test
    fun `shop item multiplier beats category regardless of order`() {
        val filter = ItemFilter(
            emptyList(), emptyList(),
            listOf(
                MultiplierRule(CategoryRule("minerals"), 2.0),
                MultiplierRule(ShopItemRule("diamond"), 0.5)
            )
        )
        assertEquals(0.5, filter.multiplierFor(diamond, stack))
    }

    @Test
    fun `parse rule syntax`() {
        assertEquals(ShopItemRule("diamond"), ItemFilter.parseRule("diamond"))
        assertEquals(CategoryRule("minerals"), ItemFilter.parseRule("category:minerals"))
        assertNull(ItemFilter.parseRule("item:ecoitems:ruby"))
        assertNull(ItemFilter.parseRule("category:"))
        assertNull(ItemFilter.parseRule(""))
    }

    @Test
    fun `parse warns on unknown ids and drops them`() {
        val config = mockk<Config> {
            every { getStrings("shops") } returns listOf("main", "missing_shop")
            every { getStrings("whitelist") } returns listOf("diamond", "missing_item", "category:missing_cat", "item:stone")
            every { getStrings("blacklist") } returns emptyList()
            every { getSubsections("multipliers") } returns emptyList()
        }
        val warnings = mutableListOf<String>()
        val filter = ItemFilter.parse(config, allowMultipliers = false, lookup = lookup) { warnings += it }

        assertEquals(4, warnings.size)
        assertTrue(filter.allows(diamond, stack))
        assertFalse(filter.allows(cobble, stack))
    }
}
