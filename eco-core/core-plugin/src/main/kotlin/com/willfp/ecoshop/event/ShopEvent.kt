package com.willfp.ecoshop.event

import com.willfp.eco.core.price.Price
import com.willfp.ecoshop.shop.ShopItem

interface ShopEvent {
    val shopItem: ShopItem
    var price: Price
}
