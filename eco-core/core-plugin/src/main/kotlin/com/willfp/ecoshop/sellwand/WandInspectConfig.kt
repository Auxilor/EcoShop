package com.willfp.ecoshop.sellwand

import com.willfp.eco.core.config.interfaces.Config

enum class InspectDisplay { MESSAGE, HOLOGRAM, BOTH }

/** Left-click preview settings for a wand type. */
data class WandInspectConfig(
    val enabled: Boolean,
    val display: InspectDisplay,
    val durationTicks: Int,
    val height: Double,
    val lines: List<String>
) {
    companion object {
        fun parse(config: Config) = WandInspectConfig(
            enabled = config.getBoolOrNull("enabled") ?: true,
            display = InspectDisplay.entries.firstOrNull { it.name.equals(config.getStringOrNull("display"), ignoreCase = true) }
                ?: InspectDisplay.MESSAGE,
            durationTicks = (config.getIntOrNull("duration") ?: 60).coerceAtLeast(1),
            height = config.getDoubleOrNull("height") ?: 1.5,
            lines = config.getStrings("lines")
        )
    }
}
