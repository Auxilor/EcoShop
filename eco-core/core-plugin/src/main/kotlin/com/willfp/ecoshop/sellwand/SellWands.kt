package com.willfp.ecoshop.sellwand

import wiki.ecohub.config.Config
import wiki.ecohub.config.category.RegistrableCategory
import wiki.ecohub.core.plugin.EcoPlugin
import wiki.ecohub.game.item.Items

object SellWands : RegistrableCategory<SellWand>("sellwand", "sellwands") {
    override fun clear(plugin: LibreforgePlugin) {
        // Drop lookup ids of wands removed from config since the last load.
        registry.values().forEach { Items.removeCustomItem(it.customItem.key) }
        registry.clear()
    }

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        registry.register(SellWand(id, config))
    }
}
