package com.willfp.ecoshop

import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.eco.core.integrations.shop.ShopManager
import com.willfp.ecoshop.commands.CommandEcoShop
import com.willfp.ecoshop.commands.CommandSell
import com.willfp.ecoshop.integrations.EcoShopAdapter
import com.willfp.ecoshop.libreforge.FilterShopItem
import com.willfp.ecoshop.libreforge.TriggerBuyItem
import com.willfp.ecoshop.libreforge.TriggerSellItem
import com.willfp.ecoshop.shop.ShopCategories
import com.willfp.ecoshop.shop.Shops
import com.willfp.ecoshop.shop.gui.SellGUI
import com.willfp.libreforge.filters.Filters
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.ConfigCategory
import com.willfp.libreforge.triggers.Triggers
import org.bukkit.event.Listener

class EcoShopPlugin : LibreforgePlugin() {
    init {
        instance = this

        ShopManager.register(EcoShopAdapter)
    }

    override fun loadConfigCategories(): List<ConfigCategory> {
        return listOf(
            ShopCategories,
            Shops
        )
    }

    override fun handleEnable() {
        SellGUI.update(this)

        Filters.register(FilterShopItem)
        Triggers.register(TriggerBuyItem)
        Triggers.register(TriggerSellItem)
    }

    override fun handleReload() {
        SellGUI.update(this)
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

    override fun getMinimumEcoVersion(): String {
        return "6.65.0"
    }

    companion object {
        /** Instance of the plugin. */
        lateinit var instance: EcoShopPlugin
            private set
    }
}
