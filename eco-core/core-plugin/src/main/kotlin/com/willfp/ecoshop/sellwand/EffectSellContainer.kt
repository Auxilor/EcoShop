package com.willfp.ecoshop.sellwand

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.sell.ItemFilter
import com.willfp.ecoshop.sell.SellBypass
import com.willfp.libreforge.ArgType
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.arguments
import com.willfp.libreforge.effects.Effect
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.TriggerParameter
import org.bukkit.block.Container

object EffectSellContainer : Effect<NoCompileData>("sell_container") {
    override val description = "Sells the sellable contents of the container at the trigger location, like a sell wand."

    override val categories = setOf("economy")

    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.LOCATION
    )

    override val arguments = arguments {
        optional("wand", description = "A sell wand ID whose filters, multiplier, caps, cooldown and period limit to use.", type = ArgType.STRING)
        optional("multiplier", description = "Sell price multiplier (ignored when wand is set).", type = ArgType.DOUBLE, default = "1")
        optional("containers", description = "Container materials this works on (ignored when wand is set).", type = ArgType.STRING_LIST)
        optional("filters", description = "Filter section: shops, whitelist, blacklist, multipliers (ignored when wand is set).", type = ArgType.ANY)
        optional("max-items", description = "Max items per use, -1 for none (ignored when wand is set).", type = ArgType.INT, default = "-1")
        optional("max-value", description = "Max economy value per use, -1 for none (ignored when wand is set).", type = ArgType.DOUBLE, default = "-1")
        optional("bypass", description = "Bypass section: dynamic-pricing, player-limits, global-limits (ignored when wand is set).", type = ArgType.ANY)
    }

    override fun onTrigger(config: Config, data: TriggerData, compileData: NoCompileData): Boolean {
        val player = data.player ?: return false
        val block = data.location?.block ?: return false
        val container = block.getState(false) as? Container ?: return false

        val wandId = config.getStringOrNull("wand")
        val profile = if (wandId != null) {
            WandProfile.of(SellWands[wandId] ?: return false)
        } else {
            inlineProfile(config)
        }

        if (!profile.containers.matches(block.type)) return false

        if (!ContainerAccess.canAccess(player, block)) {
            player.sendMessage(plugin.langYml.getMessage("sellwand.no-access"))
            return false
        }

        return WandSeller.use(player, block, container, profile, consumeFromHand = false)
    }

    private fun inlineProfile(config: Config): WandProfile {
        val containerNames = config.getStrings("containers").ifEmpty { listOf("chest", "trapped_chest", "barrel") }
        val maxItems = config.getIntOrNull("max-items") ?: -1
        val maxValue = config.getDoubleOrNull("max-value") ?: -1.0

        return WandProfile(
            wand = null,
            limits = null,
            multiplier = config.getDoubleOrNull("multiplier") ?: 1.0,
            maxItems = if (maxItems < 0) Int.MAX_VALUE else maxItems,
            maxValue = if (maxValue < 0) Double.MAX_VALUE else maxValue,
            containers = ContainerTypes(containerNames),
            filter = ItemFilter.parse(config.getSubsection("filters"), allowMultipliers = true) {
                plugin.logger.warning("[sell_container effect] $it")
            },
            bypass = SellBypass.parse(config.getSubsection("bypass")),
            conditions = null
        )
    }
}
