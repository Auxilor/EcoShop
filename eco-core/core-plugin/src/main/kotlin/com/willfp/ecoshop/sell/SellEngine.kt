package com.willfp.ecoshop.sell

import com.willfp.ecoshop.event.EcoShopContainerSellEvent
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.event.Event

data class SellLogEntry(
    val playerName: String,
    val itemId: String,
    val units: Int,
    val value: Double,
    val source: SellSource
)

/** Applies a quote: pays, removes items, records sells, fires events and effects, logs. */
class SellEngine(
    private val callEvent: (Event) -> Unit,
    private val payOffline: (OfflinePlayer, Double) -> Boolean,
    private val log: (SellLogEntry) -> Unit
) {
    fun commit(quote: SellQuote, location: Location? = null): SellResult {
        val request = quote.request
        if (quote.isEmpty) {
            return SellResult(emptyList(), emptyList(), quote.unsold)
        }

        val seller = request.seller

        if (location != null && (request.source == SellSource.WAND || request.source == SellSource.CHEST)) {
            val event = EcoShopContainerSellEvent(
                seller.owner,
                (seller as? Seller.Online)?.player,
                request.source,
                location,
                quote.lines
            )
            callEvent(event)
            if (event.isCancelled) {
                return SellResult(emptyList(), emptyList(), quote.unsold)
            }
        }

        val paid = mutableListOf<PaidLine>()
        val failed = mutableListOf<QuoteLine>()

        for (line in quote.lines) {
            val candidate = line.candidate

            val intact = line.takes.all { take ->
                val stack = request.target.get(take.index)
                stack != null && stack.amount >= take.amount && candidate.matchesForSale(stack)
            }
            if (!intact) {
                failed += line
                continue
            }

            val paidLine = when (seller) {
                is Seller.Online -> {
                    val player = seller.player
                    val first = request.target.get(line.takes.first().index)!!
                    val eventMultiplier = candidate.fireSellEvent(player, first, line.units, request.source)
                    val applied = if (request.applyEventMultiplier) eventMultiplier else 1.0
                    val multiplier = line.priceMultiplier * applied
                    candidate.giveSellPayout(player, multiplier)
                    PaidLine(candidate, line.units, multiplier, line.economyValue?.times(applied))
                }

                is Seller.Offline -> {
                    val value = line.economyValue
                    if (value == null || !payOffline(seller.owner, value)) {
                        failed += line
                        continue
                    }
                    PaidLine(candidate, line.units, line.priceMultiplier, value)
                }
            }

            for (take in line.takes) {
                request.target.remove(take.index, take.amount)
            }

            candidate.recordSell(seller.owner, line.units, request.bypass)

            if (seller is Seller.Online) {
                candidate.triggerSellEffects(seller.player, line.units)
            }

            log(
                SellLogEntry(
                    seller.owner.name ?: seller.owner.uniqueId.toString(),
                    candidate.id,
                    line.units,
                    paidLine.economyValue ?: (line.baseValue * paidLine.multiplier),
                    request.source
                )
            )

            paid += paidLine
        }

        return SellResult(paid, failed, quote.unsold)
    }
}
