package com.willfp.ecoshop.sellchest

import com.willfp.ecoshop.sell.testCandidate
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OfflineEligibilityTest {
    @Test
    fun `plain economy item is eligible`() {
        assertTrue(OfflineEligibility.isEligible(testCandidate("diamond", rawExpression = "10"), ownerAllowed = true))
    }

    @Test
    fun `owner snapshot false blocks`() {
        assertFalse(OfflineEligibility.isEligible(testCandidate("diamond", rawExpression = "10"), ownerAllowed = false))
    }

    @Test
    fun `non economy price blocks`() {
        assertFalse(OfflineEligibility.isEligible(testCandidate("d", economy = false, rawExpression = "10"), true))
    }

    @Test
    fun `placeholder in value blocks`() {
        assertFalse(OfflineEligibility.isEligible(testCandidate("d", rawExpression = "10 * %player_level%"), true))
    }

    @Test
    fun `missing expression blocks`() {
        assertFalse(OfflineEligibility.isEligible(testCandidate("d", rawExpression = null), true))
    }

    @Test
    fun `sell conditions block`() {
        assertFalse(OfflineEligibility.isEligible(testCandidate("d", hasConditions = true, rawExpression = "10"), true))
    }
}
