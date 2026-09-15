package com.willfp.ecoshop.sellchest

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.items.CustomItem
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.registry.KRegistrable
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.sell.ItemFilter
import com.willfp.ecoshop.sell.SellBypass
import org.bukkit.Material

enum class Overflow {
    KEEP,
    STOP
}

class SellChestType(
    override val id: String,
    val config: Config
) : KRegistrable {
    val displayName: String = config.getFormattedStringOrNull("name") ?: id

    val block: Material = Material.matchMaterial(config.getStringOrNull("block") ?: "chest")
        ?.takeIf { it == Material.CHEST || it == Material.TRAPPED_CHEST || it == Material.BARREL }
        ?: run {
            plugin.logger.warning("[sell chest $id] block must be chest, trapped_chest or barrel; using chest")
            Material.CHEST
        }

    val baseItem = Items.lookup(config.getString("item")).item.let { item ->
        if (item.type != block) {
            plugin.logger.warning("[sell chest $id] item material ${item.type} does not match block $block; using $block")
            item.withType(block)
        } else item
    }

    val loreTemplate: List<String> = config.getFormattedStrings("lore")

    val sellInterval = (config.getIntOrNull("sell-interval") ?: 100).coerceAtLeast(1)

    /** Set by [loadFilter] once shops are loaded; types load before shops so shop items can look them up. */
    lateinit var filter: ItemFilter
        private set

    val bypass = SellBypass.parse(config.getSubsection("bypass"))

    val overflow = if (config.getStringOrNull("overflow").equals("stop", ignoreCase = true)) Overflow.STOP else Overflow.KEEP

    val notify = config.getBoolOrNull("notify") ?: true

    val hologram = ChestHologramConfig.parse(config.getSubsection("hologram"))

    /** `ecoshop:sellchest_<id>` in eco item lookups. Declared last: [SellChestItem.create] reads the fields above. */
    val customItem = CustomItem(
        plugin.createNamespacedKey("sellchest_$id"),
        { SellChestItem.typeIdOf(it) == id },
        SellChestItem.create(this)
    ).apply { register() }

    fun loadFilter() {
        filter = ItemFilter.parse(config.getSubsection("filters"), allowMultipliers = false) {
            plugin.logger.warning("[sell chest $id] $it")
        }
    }
}
