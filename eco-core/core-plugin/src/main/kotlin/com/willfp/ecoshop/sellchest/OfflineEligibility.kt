package com.willfp.ecoshop.sellchest

import com.willfp.ecoshop.sell.SellCandidate

/** Which shop items a sell chest may sell while its owner is offline. */
object OfflineEligibility {
    fun isEligible(candidate: SellCandidate, ownerAllowed: Boolean): Boolean {
        if (!ownerAllowed) return false
        if (!candidate.isEconomyPrice()) return false
        val expression = candidate.rawSellValueExpression ?: return false
        if ('%' in expression) return false
        return !candidate.hasSellConditions
    }
}
