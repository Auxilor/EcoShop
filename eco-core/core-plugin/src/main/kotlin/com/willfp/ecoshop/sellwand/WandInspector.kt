package com.willfp.ecoshop.sellwand

import com.willfp.eco.core.integrations.hologram.Hologram
import com.willfp.eco.core.integrations.hologram.HologramManager
import com.willfp.eco.core.integrations.hologram.HologramOptions
import com.willfp.eco.util.StringUtils
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.sell.SellTotals
import com.willfp.ecoshop.sell.SellValueFormat
import com.willfp.ecoshop.sell.Sells
import org.bukkit.block.Block
import org.bukkit.block.Container
import org.bukkit.entity.Player
import java.util.UUID

/** Previews what a wand would sell, without selling. No uses, cooldown, period use or counters. */
object WandInspector {
    private val holograms = HashMap<UUID, Hologram>()

    fun inspect(player: Player, block: Block, container: Container, wand: SellWand) {
        if (!player.hasPermission("ecoshop.sellwand.use")) {
            player.sendMessage(plugin.langYml.getMessage("sellwand.no-permission"))
            return
        }
        if (!ContainerAccess.canAccess(player, block)) {
            player.sendMessage(plugin.langYml.getMessage("sellwand.no-access"))
            return
        }

        val quote = Sells.quoter.quote(WandSeller.requestFor(player, container, WandProfile.of(wand)))
        val totals = SellTotals.of(quote)
        val amount = "%,d".format(java.util.Locale.ROOT, totals.units)
        val price = SellValueFormat.format(totals, player)
        val unsold = quote.unsold.map { it.index }.distinct().size.toString()

        fun fill(text: String) = text
            .replace("%amount%", amount)
            .replace("%price%", price)
            .replace("%unsold%", unsold)

        val config = wand.inspect
        if (config.display != InspectDisplay.HOLOGRAM) {
            player.sendMessage(fill(plugin.langYml.getMessage("sellwand.inspect")))
        }
        if (config.display != InspectDisplay.MESSAGE) {
            showHologram(player, block, config, config.lines.map { StringUtils.format(fill(it), player) })
        }
    }

    private fun showHologram(player: Player, block: Block, config: WandInspectConfig, lines: List<String>) {
        holograms.remove(player.uniqueId)?.remove()

        val hologram = HologramManager.createHologram(
            block.location.add(0.5, config.height, 0.5),
            HologramOptions.builder().contents(lines).visibleByDefault(false).build()
        )
        hologram.show(player)
        holograms[player.uniqueId] = hologram

        plugin.scheduler.runLater(config.durationTicks.toLong()) {
            if (holograms.remove(player.uniqueId, hologram)) {
                hologram.remove()
            }
        }
    }

    fun clear() {
        holograms.values.forEach { it.remove() }
        holograms.clear()
    }
}
