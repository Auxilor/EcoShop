package com.willfp.ecoshop.sell

import com.willfp.ecoshop.shop.PriceDynamicConfig
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import kotlin.math.floor

/** Computes what a sell request would pay, without side effects. */
class SellQuoter(
    private val resolve: (ItemStack) -> SellCandidate?,
    private val chunkSize: () -> Int
) {
    fun quote(request: SellRequest): SellQuote {
        val seller = request.seller
        val player = (seller as? Seller.Online)?.player
        val unsold = mutableListOf<UnsoldSlot>()
        val takes = LinkedHashMap<String, MutableList<SlotTake>>()
        val candidates = HashMap<String, SellCandidate>()
        val firstStacks = HashMap<String, ItemStack>()
        var itemsLeft = request.maxItems.toLong()

        for (index in request.target.indices) {
            val stack = request.target.get(index) ?: continue
            if (stack.isEmpty || stack.amount <= 0) continue

            val candidate = resolve(stack)
            val rejection = rejection(candidate, stack, request, player)
            if (rejection != null) {
                unsold += UnsoldSlot(index, rejection)
                continue
            }
            candidate!!

            val already = takes[candidate.id]?.sumOf { it.amount } ?: 0
            val playerLeft = if (request.bypass.playerLimits) Long.MAX_VALUE
                else candidate.sellLimit.toLong() - candidate.totalSells(seller.owner) - already
            val globalLeft = if (request.bypass.globalLimits) Long.MAX_VALUE
                else candidate.globalSellLimit.toLong() - candidate.totalGlobalSells() - already
            val take = minOf(stack.amount.toLong(), playerLeft, globalLeft, itemsLeft).coerceAtLeast(0).toInt()

            if (take < stack.amount) {
                val limiting = minOf(playerLeft, globalLeft, itemsLeft)
                val reason = when (limiting) {
                    playerLeft -> UnsoldReason.PLAYER_LIMIT
                    globalLeft -> UnsoldReason.GLOBAL_LIMIT
                    else -> UnsoldReason.CAP_REACHED
                }
                unsold += UnsoldSlot(index, reason)
            }

            if (take <= 0) continue

            itemsLeft -= take
            candidates[candidate.id] = candidate
            firstStacks.putIfAbsent(candidate.id, stack)
            takes.getOrPut(candidate.id) { mutableListOf() } += SlotTake(index, take)
        }

        var valueLeft = request.maxValue
        val lines = mutableListOf<QuoteLine>()

        for ((id, requestedTakes) in takes) {
            val candidate = candidates.getValue(id)
            val base = candidate.baseSellValue(player)
            val multiplier = request.filter.multiplierFor(candidate, firstStacks.getValue(id)) * request.extraMultiplier
            val economy = candidate.isEconomyPrice()
            val requested = requestedTakes.sumOf { it.amount }

            val dynamic = if (request.bypass.dynamicPricing) null else candidate.sellDynamicConfig
            val priced = price(candidate, dynamic, base, multiplier, requested, if (economy) valueLeft else Double.MAX_VALUE)

            val kept = trimTakes(requestedTakes, priced.units)
            if (priced.units < requested) {
                val cut = requestedTakes.firstOrNull { take ->
                    (kept.firstOrNull { it.index == take.index }?.amount ?: 0) < take.amount
                }
                if (cut != null && unsold.none { it.index == cut.index }) {
                    unsold += UnsoldSlot(cut.index, UnsoldReason.CAP_REACHED)
                }
            }

            if (priced.units <= 0) continue

            val economyValue = if (economy) base * priced.multiplier else null
            if (economyValue != null) {
                valueLeft -= economyValue
            }

            lines += QuoteLine(candidate, kept, priced.units, base, priced.multiplier, economyValue)
        }

        return SellQuote(request, lines, unsold.sortedBy { it.index })
    }

    private fun rejection(
        candidate: SellCandidate?,
        stack: ItemStack,
        request: SellRequest,
        player: Player?
    ): UnsoldReason? {
        val seller = request.seller
        return when {
            candidate == null -> UnsoldReason.NOT_IN_SHOP
            !candidate.isSellable || candidate.sellPrice == null || !candidate.matchesForSale(stack) -> UnsoldReason.NOT_SELLABLE
            !request.filter.allows(candidate, stack) -> UnsoldReason.FILTERED
            seller is Seller.Offline && !seller.isEligible(candidate) -> UnsoldReason.OFFLINE_INELIGIBLE
            player != null && request.checkPermissions && !candidate.hasSellPermission(player) -> UnsoldReason.NO_PERMISSION
            player != null && request.checkConditions && !candidate.sellConditionsMet(player) -> UnsoldReason.MISSING_REQUIREMENTS
            else -> null
        }
    }

    private data class Priced(val units: Int, val multiplier: Double)

    /** Chunked dynamic pricing, stopping early if [valueBudget] would be exceeded. [dynamic] null means flat base price. */
    private fun price(
        candidate: SellCandidate,
        dynamic: PriceDynamicConfig?,
        base: Double,
        multiplier: Double,
        units: Int,
        valueBudget: Double
    ): Priced {
        val size = chunkSize().coerceAtLeast(1)
        val buys = candidate.dynamicGlobalBuys()
        val startSells = candidate.dynamicGlobalSells()

        var sold = 0
        var total = 0.0
        var budget = valueBudget

        while (sold < units) {
            var n = minOf(size, units - sold)
            val factor = DynamicPricer.sellFactor(dynamic, base, buys, startSells + sold, candidate.id)
            val unitValue = base * factor * multiplier

            if (unitValue > 0 && n * unitValue > budget) {
                n = floor(budget / unitValue).toInt().coerceAtLeast(0)
                total += n * factor * multiplier
                sold += n
                break
            }

            total += n * factor * multiplier
            budget -= n * unitValue
            sold += n
        }

        return Priced(sold, total)
    }

    private fun trimTakes(takes: List<SlotTake>, units: Int): List<SlotTake> {
        var left = units
        val result = mutableListOf<SlotTake>()
        for (take in takes) {
            if (left <= 0) break
            val amount = minOf(take.amount, left)
            result += SlotTake(take.index, amount)
            left -= amount
        }
        return result
    }
}
