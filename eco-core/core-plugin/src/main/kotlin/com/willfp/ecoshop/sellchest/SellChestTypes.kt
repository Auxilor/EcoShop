package com.willfp.ecoshop.sellchest

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.items.Items
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.RegistrableCategory

object SellChestTypes : RegistrableCategory<SellChestType>("sellchest", "sellchests") {
    override fun clear(plugin: LibreforgePlugin) {
        // Drop lookup ids of types removed from config since the last load.
        registry.values().forEach { Items.removeCustomItem(it.customItem.key) }
        registry.clear()
    }

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        registry.register(SellChestType(id, config))
    }
}
