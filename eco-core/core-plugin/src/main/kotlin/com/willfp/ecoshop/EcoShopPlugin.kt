package com.willfp.ecoshop

import com.willfp.eco.core.bstats.EcoMetricsChart
import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.eco.core.integrations.shop.ShopManager
import com.willfp.ecoshop.commands.CommandEcoShop
import com.willfp.ecoshop.commands.CommandSell
import com.willfp.ecoshop.integrations.EcoShopAdapter
import com.willfp.ecoshop.libreforge.FilterShopItem
import com.willfp.ecoshop.libreforge.TriggerBuyItem
import com.willfp.ecoshop.libreforge.TriggerSellItem
import com.willfp.ecoshop.shop.DynamicPricingDecayTask
import com.willfp.ecoshop.shop.ShopCategories
import com.willfp.ecoshop.shop.ShopItems
import com.willfp.ecoshop.shop.Shops
import com.willfp.ecoshop.shop.gui.SellGUI
import com.willfp.libreforge.filters.Filters
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.ConfigCategory
import com.willfp.libreforge.triggers.Triggers
import org.bukkit.event.Listener

internal lateinit var plugin: EcoShopPlugin
    private set

class EcoShopPlugin : LibreforgePlugin() {
    private var decayTask: DynamicPricingDecayTask? = null

    init {
        plugin = this

        ShopManager.register(EcoShopAdapter)
    }

    override fun loadConfigCategories(): List<ConfigCategory> {
        return listOf(
            ShopCategories,
            Shops
        )
    }

    override fun handleEnable() {
        SellGUI.update()

        Filters.register(FilterShopItem)
        Triggers.register(TriggerBuyItem)
        Triggers.register(TriggerSellItem)
    }

    override fun handleReload() {
        SellGUI.update()

        decayTask?.cancel()
        decayTask = DynamicPricingDecayTask().also {
            it.runTaskTimer(this, 1200L, 1200L)
        }
    }

    override fun loadListeners(): List<Listener> {
        return listOf(

        )
    }

    override fun loadPluginCommands(): List<PluginCommand> {
        return listOf(
            CommandEcoShop,
            CommandSell
        )
    }

    override fun getCustomCharts() = listOf(
        EcoMetricsChart.SingleLine("total_shops") { Shops.values().size },
        EcoMetricsChart.SingleLine("total_shop_categories") { ShopCategories.values().size },
        EcoMetricsChart.SingleLine("total_shop_items") { ShopItems.values().size },
        EcoMetricsChart.SimplePie("use_local_storage") {
            if (configYml.getBool("use-local-storage")) "local" else "shared"
        },
        EcoMetricsChart.SimplePie("register_permissions") {
            if (configYml.getBool("shop-items.register-permissions")) "enabled" else "disabled"
        },
        EcoMetricsChart.SimplePie("sell_all_button") {
            if (configYml.getBool("sell-menu.sell-all-button.enabled")) "enabled" else "disabled"
        }
    )
}
