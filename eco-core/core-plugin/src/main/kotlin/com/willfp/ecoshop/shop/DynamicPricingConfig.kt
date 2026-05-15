package com.willfp.ecoshop.shop

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.util.StringUtils

data class PriceDynamicConfig(
    val enabled: Boolean,
    val maxIncrease: Double,
    val maxDecrease: Double,
    val formula: String?
)

data class DynamicPricingConfig(
    val buy: PriceDynamicConfig,
    val altBuy: PriceDynamicConfig,
    val sell: PriceDynamicConfig
) {
    companion object {
        fun from(config: Config, parent: DynamicPricingConfig? = null): DynamicPricingConfig {
            val globalEnabled = config.getBoolOrNull("enabled") ?: false
            val globalMaxIncrease = config.getDoubleOrNull("max-increase")
            val globalMaxDecrease = config.getDoubleOrNull("max-decrease")

            fun parseType(key: String, parentType: PriceDynamicConfig?): PriceDynamicConfig {
                val enabled = globalEnabled && (config.getBoolOrNull("$key.enabled") ?: true)
                val maxIncrease = config.getDoubleOrNull("$key.max-increase")
                    ?: globalMaxIncrease
                    ?: parentType?.maxIncrease
                    ?: 2.0
                val maxDecrease = config.getDoubleOrNull("$key.max-decrease")
                    ?: globalMaxDecrease
                    ?: parentType?.maxDecrease
                    ?: 0.0
                val formula = if (enabled)
                    config.getStringOrNull("$key.formula", false, StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                        ?: parentType?.formula
                else null
                return PriceDynamicConfig(enabled, maxIncrease, maxDecrease, formula)
            }

            return DynamicPricingConfig(
                buy = parseType("buy", parent?.buy),
                altBuy = parseType("alt-buy", parent?.altBuy),
                sell = parseType("sell", parent?.sell)
            )
        }
    }
}
