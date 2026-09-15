package com.willfp.ecoshop.sell

import com.willfp.ecoshop.shop.ShopCategories
import com.willfp.ecoshop.shop.ShopItems
import com.willfp.ecoshop.shop.Shops

/** Registry access for [ItemFilter], replaced in tests. */
interface FilterLookup {
    /** Category ids placed in the shop, or null if the shop doesn't exist. */
    fun shopCategories(shopId: String): Set<String>?

    fun categoryExists(id: String): Boolean

    fun shopItemExists(id: String): Boolean

    object Live : FilterLookup {
        override fun shopCategories(shopId: String) = Shops.getByID(shopId)?.categoryIds

        override fun categoryExists(id: String) = ShopCategories.getByID(id) != null

        override fun shopItemExists(id: String) = ShopItems[id] != null
    }
}
