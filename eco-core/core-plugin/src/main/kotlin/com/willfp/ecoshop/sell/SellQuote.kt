package com.willfp.ecoshop.sell

data class SlotTake(val index: Int, val amount: Int)

/** One shop item's portion of a quote. [priceMultiplier] excludes the event multiplier. */
data class QuoteLine(
    val candidate: SellCandidate,
    val takes: List<SlotTake>,
    val units: Int,
    val baseValue: Double,
    val priceMultiplier: Double,
    val economyValue: Double?
)

enum class UnsoldReason {
    NOT_IN_SHOP,
    NOT_SELLABLE,
    FILTERED,
    NO_PERMISSION,
    MISSING_REQUIREMENTS,
    PLAYER_LIMIT,
    GLOBAL_LIMIT,
    CAP_REACHED,
    OFFLINE_INELIGIBLE
}

data class UnsoldSlot(val index: Int, val reason: UnsoldReason)

data class SellQuote(
    val request: SellRequest,
    val lines: List<QuoteLine>,
    val unsold: List<UnsoldSlot>
) {
    val isEmpty: Boolean
        get() = lines.isEmpty()
}
