package com.willfp.ecoshop.libreforge

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoshop.shop.BuyStatus
import com.willfp.ecoshop.shop.BuyType
import com.willfp.ecoshop.shop.ShopCategories
import com.willfp.ecoshop.shop.ShopItems
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.arguments
import com.willfp.libreforge.effects.Effect
import com.willfp.libreforge.getDoubleFromExpression
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.TriggerParameter

object EffectBuyItem : Effect<NoCompileData>("buy_item") {
    override val parameters = setOf(
        TriggerParameter.PLAYER
    )

    override val arguments = arguments {
        require("item", "You must specify the shop item ID!")
        require("type", "You must specify the buy type (buy or alt_buy)!")
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