package com.willfp.ecoshop.logging

import com.willfp.ecoshop.event.EcoShopBuyEvent
import com.willfp.ecoshop.event.EcoShopSellEvent
import com.willfp.ecoshop.plugin
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

object ShopLogListener : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onBuy(event: EcoShopBuyEvent) {
        if (!plugin.configYml.getBool("logging.enabled")) {
            return
        }

        ShopLogger.log(
            player = event.player.name,
            type = "BUY",
            itemId = event.shopItem.id,
            amount = event.amount,
            price = event.price.getValue(event.player) * event.payAmount
        )
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onSell(event: EcoShopSellEvent) {
        if (!plugin.configYml.getBool("logging.enabled")) {
            return
        }

        ShopLogger.log(
            player = event.player.name,
            type = "SELL",
            itemId = event.shopItem.id,
            amount = event.amount,
            price = event.price.getValue(event.player) * event.amount * event.multiplier * event.shopItem.getEffectiveSellMultiplier(event.player)
        )
    }
}
