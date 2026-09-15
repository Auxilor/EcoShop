package com.willfp.ecoshop.sellwand

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WandItemTest {
    @Test
    fun `infinite uses never decrease`() {
        assertEquals(-1, nextWandUses(-1))
    }

    @Test
    fun `finite uses decrease to zero floor`() {
        assertEquals(4, nextWandUses(5))
        assertEquals(0, nextWandUses(1))
        assertEquals(0, nextWandUses(0))
    }

    @Test
    fun `lore placeholders render`() {
        val lore = renderWandLore(listOf("Uses: %uses%/%max_uses%", "x%multiplier%"), 3, 10, 1.5)
        assertEquals(listOf("Uses: 3/10", "x1.5"), lore)
    }

    @Test
    fun `infinite uses render as infinity`() {
        assertEquals(listOf("∞/∞"), renderWandLore(listOf("%uses%/%max_uses%"), -1, -1, 1.0))
    }
}
