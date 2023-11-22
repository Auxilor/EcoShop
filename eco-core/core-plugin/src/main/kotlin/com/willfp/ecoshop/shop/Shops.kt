package com.willfp.ecoshop.shop

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoshop.EcoShopPlugin
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.RegistrableCategory

object Shops : RegistrableCategory<Shop>("shop", "shops") {
    override fun clear(plugin: LibreforgePlugin) {
        registry.clear()
    }

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        registry.register(Shop(plugin as EcoShopPlugin, id, config))
    }
}
