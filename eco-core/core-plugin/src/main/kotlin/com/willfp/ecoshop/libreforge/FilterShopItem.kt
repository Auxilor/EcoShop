package com.willfp.ecoshop.libreforge

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoshop.event.ShopEvent
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.filters.Filter
import com.willfp.libreforge.triggers.TriggerData

object FilterShopItem : Filter<NoCompileData, Collection<String>>("shop_item") {
    override fun getValue(config: Config, data: TriggerData?, key: String): Collection<String> {
        return config.getStrings(key)
    }

    override fun isMet(data: TriggerData, value: Collection<String>, compileData: NoCompileData): Boolean {
        val event = data.event as? ShopEvent ?: return true

        return value.any { shopItemName ->
            shopItemName.equals(event.shopItem.id, ignoreCase = true)
        }
    }
}
