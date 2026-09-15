package com.willfp.ecoshop.sellchest

import com.willfp.eco.core.integrations.hologram.Hologram
import com.willfp.eco.core.integrations.hologram.HologramManager
import com.willfp.eco.util.StringUtils
import com.willfp.eco.util.savedDisplayName
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.sell.SellTotals
import com.willfp.ecoshop.sell.SellValueFormat
import org.bukkit.Bukkit
import java.util.Locale
import java.util.UUID

internal fun renderChestLines(
    template: List<String>,
    typeName: String,
    ownerName: String,
    soldItems: Long,
    soldValue: String
): List<String> = template.map {
    it.replace("%type%", typeName)
        .replace("%owner%", ownerName)
        .replace("%sold_items%", "%,d".format(Locale.ROOT, soldItems))
        .replace("%sold_value%", soldValue)
}

/** One hologram per indexed sell chest whose type has holograms enabled. */
object SellChestHolograms {
    private val holograms = HashMap<ChestKey, Hologram>()

    private val globallyEnabled: Boolean
        get() = plugin.configYml.getBoolOrNull("sell-chests.holograms") ?: true

    /** Creates the chest's hologram, or updates its lines if it already exists. */
    fun show(key: ChestKey, type: SellChestType, owner: UUID, totals: SellTotals) {
        if (!globallyEnabled || !type.hologram.enabled) return
        val block = key.block() ?: return

        val lines = renderChestLines(
            type.hologram.lines,
            type.displayName,
            Bukkit.getOfflinePlayer(owner).savedDisplayName,
            totals.units,
            SellValueFormat.format(totals, null)
        ).map { StringUtils.format(it) }

        val existing = holograms[key]
        if (existing != null) {
            existing.setContents(lines)
        } else {
            holograms[key] = HologramManager.createHologram(block.location.add(0.5, type.hologram.height, 0.5), lines)
        }
    }

    fun remove(key: ChestKey) {
        holograms.remove(key)?.remove()
    }

    fun clear() {
        holograms.values.forEach { it.remove() }
        holograms.clear()
    }
}
