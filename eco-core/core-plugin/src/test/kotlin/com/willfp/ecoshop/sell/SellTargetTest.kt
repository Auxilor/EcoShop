package com.willfp.ecoshop.sell

import com.willfp.ecoshop.fakeStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SellTargetTest {
    @Test
    fun `list target removes partial and full amounts`() {
        val a = fakeStack(10)
        val b = fakeStack(3)
        val target = ListTarget(listOf(a, b))

        assertEquals(listOf(0, 1), target.indices)
        target.remove(0, 4)
        target.remove(1, 3)

        assertEquals(6, a.amount)
        assertEquals(0, b.amount)
    }
}
