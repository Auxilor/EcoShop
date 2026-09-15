package com.willfp.ecoshop.sellchest

import com.willfp.eco.core.config.interfaces.Config

data class ChestHologramConfig(
    val enabled: Boolean,
    val height: Double,
    val lines: List<String>
) {
    companion object {
        fun parse(config: Config) = ChestHologramConfig(
            enabled = config.getBoolOrNull("enabled") ?: false,
            height = config.getDoubleOrNull("height") ?: 1.5,
            lines = config.getStrings("lines")
        )
    }
}
