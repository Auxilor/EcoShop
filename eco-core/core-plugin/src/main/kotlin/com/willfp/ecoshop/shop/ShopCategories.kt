package com.willfp.ecoshop.shop

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoshop.EcoShopPlugin
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.RegistrableCategory

@Suppress("UNUSED")
object ShopCategories : RegistrableCategory<ShopCategory>("category", "categories") {
    override fun clear(plugin: LibreforgePlugin) {
        registry.clear()
    }

    override fun beforeReload(plugin: LibreforgePlugin) {
        ShopItems.clear()
    }

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        registry.register(ShopCategory(plugin as EcoShopPlugin, id, config))
    }
}
