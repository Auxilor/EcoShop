package com.willfp.ecoshop.sell

import com.willfp.eco.util.NumberUtils
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.shop.PriceDynamicConfig

/** Player-free dynamic sell pricing, shared by every sell path. */
object DynamicPricer {
    internal var evaluator: (String) -> Double = { NumberUtils.evaluateExpression(it) }
    internal var warn: (String) -> Unit = { plugin.logger.warning(it) }

    private val warned = mutableSetOf<String>()

    /** The unit sell value after dynamic pricing, given simulated [buys] and [sells] counters. */
    fun sellValue(config: PriceDynamicConfig?, baseValue: Double, buys: Int, sells: Int, warnKey: String): Double {
        val pc = config ?: return baseValue
        if (!pc.enabled) return baseValue
        val formula = pc.formula ?: return baseValue

        val substituted = formula
            .replace("%base_price%", baseValue.toString())
            .replace("%buys%", buys.toString())
            .replace("%sells%", sells.toString())

        val result = evaluator(substituted)
        if (result.isNaN() || result.isInfinite() || (result == 0.0 && baseValue != 0.0)) {
            if (warned.add("$warnKey:$formula")) {
                warn("[EcoShop] Dynamic pricing formula failed for item '$warnKey': \"$formula\"")
            }
            return baseValue
        }

        val clamped = result.coerceIn(
            minOf(baseValue * pc.maxDecrease, baseValue * pc.maxIncrease),
            maxOf(baseValue * pc.maxDecrease, baseValue * pc.maxIncrease)
        )
        return Math.round(clamped * 100) / 100.0
    }

    /** [sellValue] divided by [baseValue]; 1.0 when the base is not positive. */
    fun sellFactor(config: PriceDynamicConfig?, baseValue: Double, buys: Int, sells: Int, warnKey: String): Double {
        if (baseValue <= 0) return 1.0
        return sellValue(config, baseValue, buys, sells, warnKey) / baseValue
    }

    internal fun resetWarnings() = warned.clear()

    internal fun resetDefaults() {
        evaluator = { NumberUtils.evaluateExpression(it) }
        warn = { plugin.logger.warning(it) }
        warned.clear()
    }
}
