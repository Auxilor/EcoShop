package com.willfp.ecoshop.sellwand

import com.willfp.eco.core.sound.PlayableSound
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.sell.InventoryTarget
import com.willfp.ecoshop.sell.SellRequest
import com.willfp.ecoshop.sell.SellSource
import com.willfp.ecoshop.sell.Seller
import com.willfp.ecoshop.sell.Sells
import com.willfp.ecoshop.util.formatDuration
import com.willfp.libreforge.EmptyProvidedHolder
import com.willfp.libreforge.toDispatcher
import com.willfp.libreforge.triggers.TriggerData
import org.bukkit.block.Block
import org.bukkit.block.Container
import org.bukkit.entity.Player

/** Runs one wand use: checks, sale, and use/limit bookkeeping. Shared by the listener and the effect. */
object WandSeller {
    val limiter by lazy { WandLimiter(ProfilePeriodStore) }

    /** Returns true if anything was sold. */
    fun use(player: Player, block: Block, container: Container, profile: WandProfile, consumeFromHand: Boolean): Boolean {
        if (!player.hasPermission("ecoshop.sellwand.use")) {
            player.sendMessage(plugin.langYml.getMessage("sellwand.no-permission"))
            return false
        }

        val conditions = profile.conditions
        if (conditions != null && !conditions.areMet(player.toDispatcher(), EmptyProvidedHolder)) {
            conditions.areMetAndTrigger(TriggerData(player = player).dispatch(player.toDispatcher()))
            player.sendMessage(plugin.langYml.getMessage("sellwand.missing-requirements"))
            return false
        }

        val hand = player.inventory.itemInMainHand
        val usesBefore = if (consumeFromHand) WandItem.usesOf(hand) else -1
        if (consumeFromHand && usesBefore == 0) {
            player.sendMessage(plugin.langYml.getMessage("sellwand.no-uses"))
            return false
        }

        val limits = profile.limits
        if (limits != null) {
            when (val check = limiter.check(player, limits)) {
                is LimitCheck.OnCooldown -> {
                    player.sendMessage(
                        plugin.langYml.getMessage("sellwand.on-cooldown")
                            .replace("%time%", ((check.remainingMillis + 999) / 1000).toString())
                    )
                    return false
                }

                is LimitCheck.PeriodExhausted -> {
                    player.sendMessage(
                        plugin.langYml.getMessage("sellwand.period-limit")
                            .replace("%time%", formatDuration(check.resetsInSeconds.toLong()))
                    )
                    return false
                }

                LimitCheck.Allowed -> Unit
            }
        }

        val result = Sells.sell(
            SellRequest(
                seller = Seller.Online(player),
                source = SellSource.WAND,
                target = InventoryTarget(container.inventory),
                filter = profile.filter,
                extraMultiplier = profile.multiplier,
                applyEventMultiplier = true,
                maxItems = profile.maxItems,
                maxValue = profile.maxValue,
                bypass = profile.bypass
            ),
            location = block.location
        )

        if (result.soldUnits == 0) {
            player.sendMessage(plugin.langYml.getMessage("sellwand.nothing-sold"))
            return false
        }

        var usesAfter = usesBefore
        val wand = profile.wand
        if (consumeFromHand && wand != null && usesBefore >= 0) {
            usesAfter = nextWandUses(usesBefore)
            if (usesAfter == 0 && wand.breakWhenEmpty) {
                hand.amount -= 1
            } else {
                WandItem.setUses(hand, wand, usesAfter)
            }
            player.inventory.setItemInMainHand(hand.takeIf { it.amount > 0 })
        }

        if (limits != null) {
            limiter.recordUse(player, limits)
        }

        TriggerUseSellWand.fire(player, block.location, result.economyTotal, result.soldUnits)

        PlayableSound.create(plugin.configYml.getSubsection("sell-wands.sound"))?.playTo(player)

        player.sendMessage(
            plugin.langYml.getMessage("sellwand.sold")
                .replace("%amount%", result.soldUnits.toString())
                .replace("%price%", result.display(player))
                .replace("%uses%", if (usesAfter < 0) "∞" else usesAfter.toString())
        )

        return true
    }
}
