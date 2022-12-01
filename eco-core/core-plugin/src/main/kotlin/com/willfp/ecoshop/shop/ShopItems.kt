package com.willfp.ecoshop.shop

import com.google.common.collect.HashBiMap
import com.google.common.collect.ImmutableList

@Suppress("UNUSED")
object ShopItems {
    private val BY_ID = HashBiMap.create<String, ShopItem>()

    /**
     * Get item matching id.
     *
     * @param id The id to query.
     * @return The matching item, or null if not found.
     */
    @JvmStatic
    fun getByID(id: String): ShopItem? {
        return BY_ID[id]
    }

    /** All shop items. */
    @JvmStatic
    fun values(): List<ShopItem> {
        return ImmutableList.copyOf(BY_ID.values)
    }

    /** Add a new [item]. */
    fun addNewItem(item: ShopItem) {
        BY_ID[item.id] = item
    }

    /** Clear registry. */
    internal fun clear() {
        BY_ID.clear()
    }
}
