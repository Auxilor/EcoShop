package com.willfp.ecoshop.sellwand

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.items.CustomItem
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.registry.KRegistrable
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.sell.ItemFilter
import com.willfp.ecoshop.sell.SellBypass
import com.willfp.libreforge.ViolationContext
import com.willfp.libreforge.conditions.Conditions

data class WandLimits(
    val id: String,
    val cooldownSeconds: Int,
    val periodUses: Int,
    val periodMinutes: Int
)

class SellWand(
    override val id: String,
    val config: Config
) : KRegistrable {
    private val context = ViolationContext(plugin, "sell wand $id")

    val baseItem = Items.lookup(config.getString("item")).item

    val displayName: String = config.getFormattedStringOrNull("name") ?: id

    val loreTemplate: List<String> = config.getFormattedStrings("lore")

    val uses = config.getIntOrNull("uses") ?: -1

    val limits = WandLimits(
        id,
        config.getIntOrNull("cooldown") ?: 0,
        config.getIntOrNull("period-limit.uses") ?: -1,
        config.getIntOrNull("period-limit.period") ?: 1440
    )

    val maxItemsPerUse = (config.getIntOrNull("max-items-per-use") ?: -1).let { if (it < 0) Int.MAX_VALUE else it }

    val maxValuePerUse = (config.getDoubleOrNull("max-value-per-use") ?: -1.0).let { if (it < 0) Double.MAX_VALUE else it }

    val multiplier = config.getDoubleOrNull("multiplier") ?: 1.0

    val requireSneak = config.getBoolOrNull("require-sneak") ?: false

    val breakWhenEmpty = config.getBoolOrNull("break-when-empty") ?: true

    val containers = ContainerTypes(config.getStrings("containers"))

    /** Set by [loadFilter] once shops are loaded; wands load before shops so shop items can look them up. */
    lateinit var filter: ItemFilter
        private set

    val bypass = SellBypass.parse(config.getSubsection("bypass"))

    val conditions = Conditions.compile(
        config.getSubsections("conditions"),
        context.with("conditions")
    )

    /** `ecoshop:sellwand_<id>` in eco item lookups. Declared last: [WandItem.create] reads the fields above. */
    val customItem = CustomItem(
        plugin.createNamespacedKey("sellwand_$id"),
        { WandItem.idOf(it) == id },
        WandItem.create(this)
    ).apply { register() }

    fun loadFilter() {
        filter = ItemFilter.parse(config.getSubsection("filters"), allowMultipliers = true) {
            plugin.logger.warning("[sell wand $id] $it")
        }
    }
}
