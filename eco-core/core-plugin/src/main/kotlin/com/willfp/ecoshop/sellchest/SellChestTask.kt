package com.willfp.ecoshop.sellchest

import com.willfp.eco.core.scheduling.EcoTask
import com.willfp.eco.util.StringUtils
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.sell.InventoryTarget
import com.willfp.ecoshop.sell.SellRequest
import com.willfp.ecoshop.sell.SellResult
import com.willfp.ecoshop.sell.SellSource
import com.willfp.ecoshop.sell.SellTotals
import com.willfp.ecoshop.sell.Seller
import com.willfp.ecoshop.sell.Sells
import org.bukkit.Bukkit
import org.bukkit.block.Container
import java.util.UUID

/** Periodically sells the contents of dirty sell chests. */
object SellChestTask {
    private var task: EcoTask? = null
    private var tick = 0L
    private val warnedTypes = mutableSetOf<String>()
    private val warnedOwners = mutableSetOf<UUID>()

    fun start() {
        stop()
        val interval = (plugin.configYml.getIntOrNull("sell-chests.sweep-interval") ?: 20).coerceAtLeast(1).toLong()
        task = plugin.scheduler.global().runTimer(interval, interval) {
            tick += interval
            sweep(tick)
        }
    }

    fun stop() {
        task?.cancel()
        task = null
    }

    fun sweep(nowTick: Long) {
        var budget = plugin.configYml.getIntOrNull("sell-chests.max-chests-per-sweep") ?: 50

        for (chest in SellChestIndex.dirty()) {
            if (budget <= 0) break

            val type = SellChestTypes[chest.typeId]
            if (type == null) {
                if (warnedTypes.add(chest.typeId)) {
                    plugin.logger.warning("Sell chest type '${chest.typeId}' no longer exists; those chests will not sell.")
                }
                chest.dirty = false
                continue
            }

            if (nowTick - chest.lastSoldTick < type.sellInterval) continue

            val block = chest.key.block()
            val container = block?.getState(false) as? Container
            if (block == null || container == null || SellChestData.read(container) == null) {
                SellChestIndex.remove(chest.key)
                continue
            }

            val seller = sellerFor(chest) ?: continue

            budget--

            val result = Sells.sell(
                SellRequest(
                    seller = seller,
                    source = SellSource.CHEST,
                    target = InventoryTarget(container.inventory),
                    filter = type.filter,
                    extraMultiplier = 1.0,
                    applyEventMultiplier = false,
                    bypass = type.bypass
                ),
                location = block.location
            )

            chest.lastSoldTick = nowTick
            chest.dirty = false
            chest.blockedByUnsellable = type.overflow == Overflow.STOP && result.unsold.isNotEmpty()

            afterSale(chest, type, seller, result)
        }
    }

    /** The seller for this chest right now, or null to leave it dirty and try later. */
    private fun sellerFor(chest: IndexedChest): Seller? {
        Bukkit.getPlayer(chest.owner)?.let { return Seller.Online(it) }

        if (plugin.configYml.getBoolOrNull("sell-chests.offline-selling.enabled") != true) {
            return null
        }

        val owner = Bukkit.getOfflinePlayer(chest.owner)
        val allowed = OfflineEarnings.isAllowed(owner)
        return Seller.Offline(owner) { OfflineEligibility.isEligible(it, allowed) }
    }

    private fun recordStats(chest: IndexedChest, type: SellChestType, result: SellResult) {
        val block = chest.key.block() ?: return
        val totals = SellChestData.readTotals(block) + SellTotals.of(result)
        SellChestData.writeTotals(block, totals)
        SellChestHolograms.show(chest.key, type, chest.owner, totals)
    }

    private fun afterSale(chest: IndexedChest, type: SellChestType, seller: Seller, result: SellResult) {
        if (result.soldUnits > 0) {
            recordStats(chest, type, result)
        }

        if (seller is Seller.Offline) {
            if (result.soldUnits > 0) {
                OfflineEarnings.add(seller.owner, result.economyTotal, result.soldUnits)
            }
            if (result.failed.isNotEmpty() && warnedOwners.add(chest.owner)) {
                plugin.logger.warning(
                    "Offline payout failed for ${seller.owner.name ?: chest.owner}; " +
                        "your economy plugin may not support offline deposits."
                )
            }
            return
        }

        if (result.soldUnits == 0) return

        val player = (seller as Seller.Online).player
        if (!type.notify) return

        chest.sellsSinceNotify++
        val every = plugin.configYml.getIntOrNull("sell-chests.notify-every") ?: 5
        if (chest.sellsSinceNotify < every) return
        chest.sellsSinceNotify = 0

        player.sendActionBar(
            StringUtils.toComponent(
                plugin.langYml.getFormattedString("messages.sellchest.sold")
                    .replace("%amount%", result.soldUnits.toString())
                    .replace("%price%", result.display(player))
            )
        )
    }
}
