package com.willfp.ecoshop.sell

import com.willfp.eco.core.Eco
import com.willfp.eco.util.NumberUtils
import com.willfp.eco.util.StringUtils
import com.willfp.ecoshop.shop.ShopItems
import com.willfp.ecoshop.shop.formatMultiple
import org.bukkit.entity.Player

internal fun renderValues(totals: SellTotals, formatFor: (String) -> String?): List<String> =
    totals.values.map { (type, value) ->
        (formatFor(type) ?: "%value%")
            .replace("%value%", NumberUtils.format(value))
            .replace("%value_commas%", NumberUtils.formatWithCommas(value))
    }

/** Renders [SellTotals] values with each price type's display format from shop config. */
object SellValueFormat {
    /** Display format for a price type, or null if no shop item sells for it. Replaceable in tests. */
    var formatFor: (String) -> String? = ::shopFormat

    /** e.g. "$1,500 and 6 Gems". [player] null (offline owner, holograms) skips player placeholders. */
    fun format(totals: SellTotals, player: Player?): String {
        val parts = renderValues(totals, formatFor).ifEmpty { listOf(NumberUtils.format(0.0)) }
        return StringUtils.format(parts.formatMultiple(), player, StringUtils.FormatOption.WITH_PLACEHOLDERS)
    }

    private fun shopFormat(type: String): String? {
        val item = ShopItems.values().firstOrNull { it.sellPrice?.identifier == type } ?: return null
        val priceType = item.config.getString("sell.type")
        return Eco.get().ecoPlugin.langYml.getSubsections("price-display")
            .firstOrNull { it.getString("type").equals(priceType, ignoreCase = true) }
            ?.getString("display")
            ?: item.config.getStringOrNull("sell.display")
    }
}
