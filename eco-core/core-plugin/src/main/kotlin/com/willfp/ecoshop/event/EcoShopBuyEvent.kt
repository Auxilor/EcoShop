package com.willfp.ecoshop.event

import com.willfp.eco.core.price.Price
import com.willfp.ecoshop.shop.BuyType
import com.willfp.ecoshop.shop.ShopItem
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

class EcoShopBuyEvent(
    who: Player,
    override val shopItem: ShopItem,
    override var price: Price,
    val buyType: BuyType
) : PlayerEvent(who), ShopEvent {

    // Below here is bukkit boilerplate
    override fun getHandlers(): HandlerList {
        return HANDLERS
    }

    companion object {
        @JvmStatic
        private val HANDLERS: HandlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return HANDLERS
        }
    }
}
