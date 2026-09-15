package com.willfp.ecoshop.logging

import com.willfp.ecoshop.event.EcoShopBuyEvent
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
}
