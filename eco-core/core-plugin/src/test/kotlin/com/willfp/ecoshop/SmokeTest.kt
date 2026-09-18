package com.willfp.ecoshop

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SmokeTest {
    @Test
    fun `fake stack tracks amount`() {
        val stack = fakeStack(5)
        stack.amount = 2
        assertEquals(2, stack.amount)
    }
}
