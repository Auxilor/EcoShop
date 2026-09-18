package com.willfp.ecoshop.libreforge

/**
 * Schema for a single sell multiplier subsection (see [EffectSellContainer]).
 *
 * Documentation-only: parsed from source by the wiki scanner, never instantiated at runtime.
 * Non-null properties are required keys; nullable properties are optional keys.
 *
 * @property rule A shop item ID, or a category as category:<id>.
 * @property multiplier The sell price multiplier for items matching the rule. Defaults to 1.
 */
data class SellMultiplierSpec(
    val rule: String,
    val multiplier: Double?,
)
