package com.willfp.ecoshop.sell

import com.willfp.eco.core.price.CombinedDisplayPrice
import com.willfp.eco.util.formatEco
import com.willfp.ecoshop.shop.formatMultiple
import org.bukkit.entity.Player

data class PaidLine(
    val candidate: SellCandidate,
    val units: Int,
    val multiplier: Double,
    val economyValue: Double?
)

/** What a commit actually did. */
data class SellResult(
    val paid: List<PaidLine>,
    val failed: List<QuoteLine>,
    val unsold: List<UnsoldSlot>
) {
    val soldUnits: Int
        get() = paid.sumOf { it.units }

    val economyTotal: Double
        get() = paid.sumOf { it.economyValue ?: 0.0 }

    /** Combined price display for messages, e.g. "$120 and 3 Crystals". */
    fun display(player: Player): String {
        val builder = CombinedDisplayPrice.builder(player)
        for (line in paid) {
            val price = line.candidate.sellPrice ?: continue
            builder.add(price, line.multiplier)
        }
        return builder.build().displayStrings.toList().formatMultiple().formatEco(player)
    }

    companion object {
        val EMPTY = SellResult(emptyList(), emptyList(), emptyList())
    }
}
