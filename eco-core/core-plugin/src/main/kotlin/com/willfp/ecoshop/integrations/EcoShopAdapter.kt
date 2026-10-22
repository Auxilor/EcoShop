package com.willfp.ecoshop.integrations

import com.willfp.eco.core.integrations.shop.ShopIntegration
import com.willfp.eco.core.integrations.shop.ShopSellEvent
import com.willfp.eco.core.price.Price
import com.willfp.ecoshop.event.EcoShopSellEvent
import com.willfp.ecoshop.shop.getUnitSellValue
import com.willfp.ecoshop.shop.isSellable
import com.willfp.ecoshop.shop.shopItem
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack

object EcoShopAdapter : ShopIntegration {
    override fun getUnitValue(itemStack: ItemStack, player: Player): Price {
        val factor = itemStack.shopItem?.getEffectiveSellMultiplier(player) ?: 1.0
        return DynamicSellPrice(itemStack.getUnitSellValue(player), factor)
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

private class DynamicSellPrice(
    private val price: Price,
    private val factor: Double
) : Price {
    override fun canAfford(player: Player, multiplier: Double) = price.canAfford(player, multiplier * factor)

    override fun pay(player: Player, multiplier: Double) = price.pay(player, multiplier * factor)

    override fun giveTo(player: Player, multiplier: Double) = price.giveTo(player, multiplier * factor)

    override fun getValue(player: Player, multiplier: Double) = price.getValue(player, multiplier * factor)

    override fun getMultiplier(player: Player) = price.getMultiplier(player)

    override fun setMultiplier(player: Player, multiplier: Double) = price.setMultiplier(player, multiplier)

    override fun getIdentifier(): String = price.identifier
}
