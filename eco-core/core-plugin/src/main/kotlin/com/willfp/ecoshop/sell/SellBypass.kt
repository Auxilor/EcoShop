package com.willfp.ecoshop.sell

import wiki.ecohub.config.Config

/**
 * Checks a sell skips. A bypassed check is skipped entirely: the sale ignores it
 * and does not write the matching counter.
 */
data class SellBypass(
    val dynamicPricing: Boolean = false,
    val playerLimits: Boolean = false,
    val globalLimits: Boolean = false
) {
    companion object {
        val NONE = SellBypass()

        /** Reads a `bypass` section; missing keys are false. */
        fun parse(config: Config) = SellBypass(
            dynamicPricing = config.getBoolOrNull("dynamic-pricing") ?: false,
            playerLimits = config.getBoolOrNull("player-limits") ?: false,
            globalLimits = config.getBoolOrNull("global-limits") ?: false
        )
    }
}
