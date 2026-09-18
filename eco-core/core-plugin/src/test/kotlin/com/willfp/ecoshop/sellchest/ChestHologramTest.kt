package com.willfp.ecoshop.sellchest

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ChestHologramTest {
    @Test
    fun `placeholders render`() {
        val lines = renderChestLines(
            listOf("%type% (%owner%)", "Sold %sold_items%", "Earned %sold_value%"),
            typeName = "Sell Chest", ownerName = "Steve", soldItems = 1234567, soldValue = "$5"
        )
        assertEquals(listOf("Sell Chest (Steve)", "Sold 1,234,567", "Earned $5"), lines)
    }
}
