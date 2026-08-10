package com.willfp.ecoshop.logging

import com.willfp.ecoshop.event.EcoShopBuyEvent
import com.willfp.ecoshop.event.EcoShopSellEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object ShopLogListener : Listener {
    @EventHandler
    fun onBuy(event: EcoShopBuyEvent) {
        ShopLogger.log(
            player = event.player.name,
            type = "BUY",
            itemId = event.shopItem.id,
            amount = event.amount,
            price = event.price.getValue(event.player) * event.amount
        )
    }

    @EventHandler
    fun onSell(event: EcoShopSellEvent) {
        ShopLogger.log(
            player = event.player.name,
            type = "SELL",
            itemId = event.shopItem.id,
            amount = event.amount,
            price = event.price.getValue(event.player) * event.amount
        )
    }
}
