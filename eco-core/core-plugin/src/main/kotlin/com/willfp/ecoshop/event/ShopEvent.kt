package com.willfp.ecoshop.event

import com.willfp.ecoshop.shop.ShopItem
import wiki.ecohub.core.price.Price

interface ShopEvent {
    val shopItem: ShopItem
    var price: Price
}
