package com.willfp.ecoshop.shop

import com.google.common.collect.HashBiMap
import com.google.common.collect.ImmutableList
import com.willfp.ecoshop.EcoShopPlugin

@Suppress("UNUSED")
object ShopCategories {
    private val BY_ID = HashBiMap.create<String, ShopCategory>()

    /**
     * Get category matching id.
     *
     * @param id The id to query.
     * @return The matching category, or null if not found.
     */
    @JvmStatic
    fun getByID(id: String): ShopCategory? {
        return BY_ID[id]
    }

    /** All shop categories. */
    @JvmStatic
    fun values(): List<ShopCategory> {
        return ImmutableList.copyOf(BY_ID.values)
    }

    @JvmStatic
    internal fun update(plugin: EcoShopPlugin) {
        ShopItems.clear()
        BY_ID.clear()

        for ((id, config) in plugin.getConfigs("categories")) {
            BY_ID[id] = ShopCategory(plugin, id, config)
        }
    }
}
