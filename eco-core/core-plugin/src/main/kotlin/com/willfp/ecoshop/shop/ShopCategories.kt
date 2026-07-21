package com.willfp.ecoshop.shop

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.RegistrableCategory

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

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        registry.register(ShopCategory(id, config))
    }
}
