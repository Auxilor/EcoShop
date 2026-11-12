package com.willfp.ecoshop.sellchest

import wiki.ecohub.config.Config
import wiki.ecohub.config.category.RegistrableCategory
import wiki.ecohub.core.plugin.EcoPlugin
import wiki.ecohub.game.item.Items

object SellChestTypes : RegistrableCategory<SellChestType>("sellchest", "sellchests") {
    override fun clear(plugin: EcoPlugin) {
        // Drop lookup ids of types removed from config since the last load.
        registry.values().forEach { Items.removeCustom(it.customItem.getKey()) }
        registry.clear()
    }

    override fun acceptConfig(plugin: EcoPlugin, id: String, config: Config) {
        registry.register(SellChestType(id, config))
    }
}
