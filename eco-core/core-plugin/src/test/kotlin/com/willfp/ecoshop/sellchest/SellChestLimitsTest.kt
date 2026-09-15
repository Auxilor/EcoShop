package com.willfp.ecoshop.sellchest

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SellChestLimitsTest {
    @Test
    fun `default when no permissions`() {
        assertEquals(5, resolveChestLimit(listOf("some.other"), 5))
    }

    @Test
    fun `highest limit permission wins`() {
        assertEquals(20, resolveChestLimit(listOf("ecoshop.sellchest.limit.10", "ecoshop.sellchest.limit.20"), 5))
    }

    @Test
    fun `permission can lower below default`() {
        assertEquals(2, resolveChestLimit(listOf("ecoshop.sellchest.limit.2"), 5))
    }

    @Test
    fun `negative means unlimited`() {
        assertEquals(-1, resolveChestLimit(listOf("ecoshop.sellchest.limit.10", "ecoshop.sellchest.limit.-1"), 5))
        assertEquals(-1, resolveChestLimit(emptyList(), -1))
    }

    @Test
    fun `invalid suffix ignored`() {
        assertEquals(5, resolveChestLimit(listOf("ecoshop.sellchest.limit.lots"), 5))
    }

    @Test
    fun `can place respects limit`() {
        assertTrue(canPlaceChest(4, 5))
        assertFalse(canPlaceChest(5, 5))
        assertTrue(canPlaceChest(999, -1))
    }
}
