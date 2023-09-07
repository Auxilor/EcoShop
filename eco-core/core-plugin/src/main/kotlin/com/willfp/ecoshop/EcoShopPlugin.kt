package com.willfp.ecoshop

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.eco.core.config.ConfigType
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.config.readConfig
import com.willfp.eco.core.integrations.IntegrationLoader
import com.willfp.eco.core.integrations.shop.ShopManager
import com.willfp.ecoshop.commands.CommandEcoShop
import com.willfp.ecoshop.commands.CommandSell
import com.willfp.ecoshop.config.UsermadeConfig
import com.willfp.ecoshop.integration.EcoShopAdapter
import com.willfp.ecoshop.integration.libreforge.LibreforgeIntegration
import com.willfp.ecoshop.shop.Shops
import com.willfp.ecoshop.shop.gui.SellGUI
import org.bukkit.event.Listener
import java.io.File
import java.util.zip.ZipFile

class EcoShopPlugin : EcoPlugin() {
    init {
        instance = this

        ShopManager.register(EcoShopAdapter)
    }

    override fun handleEnable() {
        copyConfigs("categories")
        copyConfigs("shops")
        Shops.update(this)
        SellGUI.update(this)
    }

    override fun handleReload() {
        Shops.update(this)
        SellGUI.update(this)
    }

    private fun copyConfigs(directory: String) {
        val folder = File(this.dataFolder, directory)
        if (!folder.exists()) {
            val files = mutableListOf<String>()

            try {
                for (entry in ZipFile(this.file).entries().asIterator()) {
                    if (entry.name.startsWith("$directory/")) {
                        files.add(entry.name.removePrefix("$directory/"))
                    }
                }
            } catch (_: Exception) {
                // Sometimes, ZipFile likes to completely fail. No idea why, but here's the 'solution'!
            }

            files.removeIf { !it.endsWith(".yml") }
            files.replaceAll { it.replace(".yml", "") }

            for (configName in files) {
                UsermadeConfig(configName, directory, this)
            }
        }
    }

    fun getConfigs(directory: String): Map<String, Config> {
        val configs = mutableMapOf<String, Config>()

        for (file in File(this.dataFolder, directory).walk()) {
            if (file.nameWithoutExtension == "_example") {
                continue
            }

            if (!file.name.endsWith(".yml")) {
                continue
            }


            val id = file.nameWithoutExtension
            val config = file.readConfig(ConfigType.YAML)
            configs[id] = config
        }

        return configs
    }

    override fun loadListeners(): List<Listener> {
        return listOf(

        )
    }

    override fun loadPluginCommands(): List<PluginCommand> {
        return listOf(
            CommandEcoShop(this),
            CommandSell(this)
        )
    }

    override fun loadIntegrationLoaders(): List<IntegrationLoader> {
        return listOf(
            IntegrationLoader("libreforge") { LibreforgeIntegration.load() }
        )
    }

    override fun getMinimumEcoVersion(): String {
        return "6.48.0"
    }

    companion object {
        /** Instance of the plugin. */
        lateinit var instance: EcoShopPlugin
            private set
    }
}
