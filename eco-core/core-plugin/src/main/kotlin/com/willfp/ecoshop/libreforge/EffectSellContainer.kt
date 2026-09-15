package com.willfp.ecoshop.libreforge

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.sell.ItemFilter
import com.willfp.ecoshop.sell.SellBypass
import com.willfp.ecoshop.sellwand.ContainerAccess
import com.willfp.ecoshop.sellwand.ContainerTypes
import com.willfp.ecoshop.sellwand.SellWands
import com.willfp.ecoshop.sellwand.WandProfile
import com.willfp.ecoshop.sellwand.WandSeller
import com.willfp.libreforge.ArgType
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.arguments
import com.willfp.libreforge.effects.Effect
import com.willfp.libreforge.getDoubleFromExpression
import com.willfp.libreforge.getIntFromExpression
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.TriggerParameter
import org.bukkit.Material
import org.bukkit.block.Container

object EffectSellContainer : Effect<NoCompileData>("sell_container") {
    override val description = "Sells the sellable contents of the container at the trigger location, like a sell wand."

    override val categories = setOf("economy")

    override val additionalInfo = listOf(
        "Per-item wand uses don't apply; use the effect's own cooldown and limit arguments instead.",
        "When wand is set, that wand's settings, conditions, cooldown and period limit are used, and every other argument is ignored."
    )

    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.LOCATION
    )

    override val arguments = arguments {
        optional(
            "wand",
            description = "The ID of a sell wand to copy the filters, multiplier, caps, containers, cooldown and period limit from.",
            type = ArgType.STRING,
            example = "golden"
        )
        optional(
            "multiplier",
            description = "The sell price multiplier. Supports expressions.",
            type = ArgType.EXPRESSION,
            default = "1",
            example = "1 + %level% * 0.1"
        )
        optional(
            "containers",
            description = "The container blocks this works on. shulker_box matches every colour.",
            type = ArgType.STRING_LIST,
            default = "[chest, trapped_chest, barrel]",
            example = listOf("chest", "barrel", "shulker_box"),
            enumClass = Material::class
        )
        inherit("filters") { SellFilterArguments }
        describeInherit(
            "filters",
            "Which items to sell: shops, whitelist, blacklist, and extra multipliers for matching items."
        )
        optional(
            "max-items",
            description = "The max items sold per use, or -1 for no limit. Supports expressions.",
            type = ArgType.EXPRESSION,
            default = "-1",
            example = "1728"
        )
        optional(
            "max-value",
            description = "The max money earned per use, or -1 for no limit. Only counts eco:economy prices. Supports expressions.",
            type = ArgType.EXPRESSION,
            default = "-1",
            example = "50000"
        )
        inherit("bypass") { SellBypassArguments }
        describeInherit(
            "bypass",
            "Shop checks to skip: dynamic-pricing, player-limits and global-limits. A bypassed check is ignored, and the sale doesn't count towards it."
        )
    }

    override fun onTrigger(config: Config, data: TriggerData, compileData: NoCompileData): Boolean {
        val player = data.player ?: return false
        val block = data.location?.block ?: return false
        val container = block.getState(false) as? Container ?: return false

        val wandId = config.getStringOrNull("wand")
        val profile = if (wandId != null) {
            WandProfile.of(SellWands[wandId] ?: return false)
        } else {
            inlineProfile(config, data)
        }

        if (!profile.containers.matches(block.type)) return false

        if (!ContainerAccess.canAccess(player, block)) {
            player.sendMessage(plugin.langYml.getMessage("sellwand.no-access"))
            return false
        }

        return WandSeller.use(player, block, container, profile, consumeFromHand = false)
    }

    private fun inlineProfile(config: Config, data: TriggerData): WandProfile {
        val containerNames = config.getStrings("containers").ifEmpty { listOf("chest", "trapped_chest", "barrel") }
        val maxItems = if (config.has("max-items")) config.getIntFromExpression("max-items", data) else -1
        val maxValue = if (config.has("max-value")) config.getDoubleFromExpression("max-value", data) else -1.0

        return WandProfile(
            wand = null,
            limits = null,
            multiplier = if (config.has("multiplier")) config.getDoubleFromExpression("multiplier", data) else 1.0,
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
