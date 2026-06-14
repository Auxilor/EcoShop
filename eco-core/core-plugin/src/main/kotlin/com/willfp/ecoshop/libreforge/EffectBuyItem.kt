package com.willfp.ecoshop.libreforge

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoshop.shop.BuyStatus
import com.willfp.ecoshop.shop.BuyType
import com.willfp.ecoshop.shop.ShopCategories
import com.willfp.ecoshop.shop.ShopItems
import com.willfp.libreforge.ArgType
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.arguments
import com.willfp.libreforge.effects.Effect
import com.willfp.libreforge.getDoubleFromExpression
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.TriggerParameter

object EffectBuyItem : Effect<NoCompileData>("buy_item") {
    override val description = "Buys a shop item on the player's behalf, as if they had purchased it through the shop."

    override val categories = setOf("economy")

    override val additionalInfo = listOf(
        "Fails silently if the player cannot afford the item or has reached its purchase limits, unless bypass-limits is true."
    )

    override val parameters = setOf(
        TriggerParameter.PLAYER
    )

    override val arguments = arguments {
        require(
            "item",
            "You must specify the shop item ID!",
            description = "The ID of the shop item to buy.",
            type = ArgType.STRING
        )
        require(
            "type",
            "You must specify the buy type (buy or alt_buy)!",
            description = "Which price to buy the item with.",
            type = ArgType.STRING,
            choices = listOf("buy", "alt_buy")
        )
        optional(
            "category",
            description = "If set, the purchase only succeeds if the shop item belongs to this category.",
            type = ArgType.STRING
        )
        optional(
            "amount",
            description = "The number of the item to buy. Supports expressions.",
            type = ArgType.EXPRESSION,
            default = "1"
        )
        optional(
            "price",
            description = "Overrides the price charged for the purchase. Supports expressions.",
            type = ArgType.EXPRESSION
        )
        optional(
            "bypass-limits",
            description = "If true, ignores purchase limits and affordability checks, only requiring that the buy type has a price configured.",
            type = ArgType.BOOLEAN,
            default = "false"
        )
    }

    override fun onTrigger(config: Config, data: TriggerData, compileData: NoCompileData): Boolean {
        val player = data.player ?: return false

        val shopItem = ShopItems[config.getString("item")] ?: return false

        if (config.has("category")) {
            val category = ShopCategories.getByID(config.getString("category")) ?: return false
            if (!category.items.contains(shopItem)) return false
        }

        val buyType = when (config.getString("type").lowercase()) {
            "buy" -> BuyType.NORMAL
            "alt_buy" -> BuyType.ALT
            else -> return false
        }

        val amount = if (config.has("amount")) {
            config.getDoubleFromExpression("amount", data).toInt().coerceAtLeast(1)
        } else {
            1
        }

        val priceOverride = if (config.has("price")) {
            config.getDoubleFromExpression("price", data)
        } else {
            null
        }

        val bypassLimits = config.getBoolOrNull("bypass-limits") ?: false

        if (!bypassLimits) {
            if (shopItem.getBuyStatus(player, amount, buyType) != BuyStatus.ALLOW) return false
        } else {
            if (shopItem.getBuyPrice(buyType) == null) return false
        }

        shopItem.buy(player, amount, buyType, bypassLimits = bypassLimits, priceValueOverride = priceOverride)

        return true
    }
}