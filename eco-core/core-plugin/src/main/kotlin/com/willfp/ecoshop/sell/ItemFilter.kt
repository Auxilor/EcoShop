package com.willfp.ecoshop.sell

import com.willfp.eco.core.config.interfaces.Config
import org.bukkit.inventory.ItemStack

/** A single filter rule. Higher [specificity] wins when several multiplier rules match. */
sealed interface FilterRule {
    val id: String

    val label: String

    val specificity: Int

    fun matches(candidate: SellCandidate): Boolean
}

data class ShopItemRule(override val id: String) : FilterRule {
    override val label = "shop item"

    override val specificity = 2

    override fun matches(candidate: SellCandidate) = candidate.id == id
}

data class CategoryRule(override val id: String) : FilterRule {
    override val label = "category"

    override val specificity = 1

    override fun matches(candidate: SellCandidate) = id in candidate.categoryIds
}

data class MultiplierRule(val rule: FilterRule, val multiplier: Double)

/** Shop scope, whitelist, blacklist and per-rule multipliers for wand and chest sales. */
class ItemFilter(
    private val whitelist: List<FilterRule>,
    private val blacklist: List<FilterRule>,
    private val multipliers: List<MultiplierRule>,
    private val shops: Set<String> = emptySet(),
    private val lookup: FilterLookup = FilterLookup.Live
) {
    val hasMultipliers: Boolean
        get() = multipliers.isNotEmpty()

    fun allows(candidate: SellCandidate, stack: ItemStack): Boolean {
        if (shops.isNotEmpty() && shops.none { inShop(candidate, it) }) {
            return false
        }
        if (blacklist.any { it.matches(candidate) }) {
            return false
        }
        return whitelist.isEmpty() || whitelist.any { it.matches(candidate) }
    }

    fun multiplierFor(candidate: SellCandidate, stack: ItemStack): Double {
        return multipliers
            .filter { it.rule.matches(candidate) }
            .maxByOrNull { it.rule.specificity }
            ?.multiplier ?: 1.0
    }

    private fun inShop(candidate: SellCandidate, shopId: String): Boolean {
        val categories = lookup.shopCategories(shopId) ?: return false
        return candidate.categoryIds.any { it in categories }
    }

    companion object {
        private const val CATEGORY_PREFIX = "category:"

        val NONE = ItemFilter(emptyList(), emptyList(), emptyList())

        fun parseRule(raw: String): FilterRule? {
            if (raw.startsWith(CATEGORY_PREFIX)) {
                return raw.removePrefix(CATEGORY_PREFIX).takeIf { it.isNotEmpty() }?.let { CategoryRule(it) }
            }
            if (raw.isEmpty() || ':' in raw) return null
            return ShopItemRule(raw)
        }

        fun parse(
            config: Config,
            allowMultipliers: Boolean,
            lookup: FilterLookup = FilterLookup.Live,
            warn: (String) -> Unit
        ): ItemFilter {
            fun validRule(raw: String, key: String): FilterRule? {
                val rule = parseRule(raw) ?: run {
                    warn("Invalid filter rule '$raw' in $key")
                    return null
                }
                val exists = when (rule) {
                    is ShopItemRule -> lookup.shopItemExists(rule.id)
                    is CategoryRule -> lookup.categoryExists(rule.id)
                }
                if (!exists) {
                    warn("Unknown ${rule.label} '${rule.id}' in $key")
                    return null
                }
                return rule
            }

            fun rules(key: String) = config.getStrings(key).mapNotNull { validRule(it, key) }

            val shops = config.getStrings("shops").filter { shopId ->
                (lookup.shopCategories(shopId) != null).also { if (!it) warn("Unknown shop '$shopId' in shops") }
            }.toSet()

            val multiplierConfigs = config.getSubsections("multipliers")
            if (!allowMultipliers && multiplierConfigs.isNotEmpty()) {
                warn("Filter multipliers are not supported here and will be ignored")
            }

            val multipliers = if (allowMultipliers) {
                multiplierConfigs.mapNotNull { section ->
                    val rule = validRule(section.getString("rule"), "multipliers") ?: return@mapNotNull null
                    MultiplierRule(rule, section.getDoubleOrNull("multiplier") ?: 1.0)
                }
            } else emptyList()

            return ItemFilter(rules("whitelist"), rules("blacklist"), multipliers, shops, lookup)
        }
    }
}
