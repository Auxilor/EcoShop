package com.willfp.ecoshop.shop

import wiki.ecohub.config.Config
import wiki.ecohub.config.category.RegistrableCategory
import wiki.ecohub.core.plugin.EcoPlugin

@Suppress("UNUSED")
object ShopCategories : RegistrableCategory<ShopCategory>("category", "categories") {
    override fun clear(plugin: LibreforgePlugin) {
        registry.clear()
    }

    override fun beforeReload(plugin: LibreforgePlugin) {
        // Persist + stop schedulers on the OLD categories before they are cleared.
        // Runs before clear(), so values() still holds the previous instances.
        values().forEach { it.stopRotation() }
        ShopItems.clear()
    }

    override fun afterReload(plugin: LibreforgePlugin) {
        clearShopItemCache()
    }

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        registry.register(ShopCategory(id, config))
    }
}
