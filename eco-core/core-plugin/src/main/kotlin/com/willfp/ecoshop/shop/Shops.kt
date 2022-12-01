package com.willfp.ecoshop.shop

import com.google.common.collect.HashBiMap
import com.google.common.collect.ImmutableList
import com.willfp.eco.core.config.updating.ConfigUpdater
import com.willfp.ecoshop.EcoShopPlugin

@Suppress("UNUSED")
object Shops {
    private val BY_ID = HashBiMap.create<String, Shop>()

    /**
     * Get shop matching id.
     *
     * @param id The id to query.
     * @return The matching shop, or null if not found.
     */
    @JvmStatic
    fun getByID(id: String): Shop? {
        return BY_ID[id]
    }

    /**
     * List of all shops.
     *
     * @return The shops.
     */
    @JvmStatic
    fun values(): List<Shop> {
        return ImmutableList.copyOf(BY_ID.values)
    }

    @ConfigUpdater
    @JvmStatic
    fun update(plugin: EcoShopPlugin) {
        ShopCategories.update(plugin)

        for (shop in values()) {
            shop.remove()
        }

        BY_ID.clear()

        for ((id, config) in plugin.getConfigs("shops")) {
            BY_ID[id] = Shop(plugin, id, config)
        }
    }
}
