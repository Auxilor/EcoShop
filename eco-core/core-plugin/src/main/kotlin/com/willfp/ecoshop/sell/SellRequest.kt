package com.willfp.ecoshop.sell

/** Input to [SellQuoter]. */
data class SellRequest(
    val seller: Seller,
    val source: SellSource,
    val target: SellTarget,
    val filter: ItemFilter = ItemFilter.NONE,
    val extraMultiplier: Double = 1.0,
    val applyEventMultiplier: Boolean = true,
    val maxItems: Int = Int.MAX_VALUE,
    val maxValue: Double = Double.MAX_VALUE,
    val checkPermissions: Boolean = true,
    val checkConditions: Boolean = true,
    val bypass: SellBypass = SellBypass.NONE
)
