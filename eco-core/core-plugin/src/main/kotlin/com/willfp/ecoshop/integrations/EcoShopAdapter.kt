package com.willfp.ecoshop.integrations

import com.willfp.eco.core.integrations.shop.ShopIntegration
import com.willfp.eco.core.integrations.shop.ShopSellEvent
import com.willfp.eco.core.price.Price
import com.willfp.ecoshop.event.EcoShopSellEvent
import com.willfp.ecoshop.shop.getUnitSellValue
import com.willfp.ecoshop.shop.isSellable
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack

object EcoShopAdapter : ShopIntegration {
    override fun getUnitValue(itemStack: ItemStack, player: Player): Price {
        return itemStack.getUnitSellValue(player)
    }

    override fun getSellEventAdapter(): Listener {
        return SellEventAdapter
    }

    override fun isSellable(itemStack: ItemStack, player: Player): Boolean {
        return itemStack.isSellable(player)
    }

    override fun getPluginName() = "EcoShop"

    private object SellEventAdapter : Listener {
        @EventHandler
        fun adaptShopSellEvent(event: EcoShopSellEvent) {
            val ecoEvent = ShopSellEvent(event.player, event.price, event.item)
            Bukkit.getPluginManager().callEvent(ecoEvent)

            event.multiplier = ecoEvent.multiplier
            event.price = ecoEvent.value
        }
    }
}
