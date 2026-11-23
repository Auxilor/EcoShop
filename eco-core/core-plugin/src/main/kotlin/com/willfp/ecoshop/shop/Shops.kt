package com.willfp.ecoshop.shop

import wiki.ecohub.config.Config
import wiki.ecohub.config.category.RegistrableCategory
import wiki.ecohub.core.plugin.EcoPlugin

object Shops : RegistrableCategory<Shop>("shop", "shops") {
    override fun clear(plugin: EcoPlugin) {
        for (shop in registry.values()) {
            shop.remove()
        }

        registry.clear()
    }

    override fun acceptConfig(plugin: EcoPlugin, id: String, config: Config) {
        registry.register(Shop(id, config))
    }
}
