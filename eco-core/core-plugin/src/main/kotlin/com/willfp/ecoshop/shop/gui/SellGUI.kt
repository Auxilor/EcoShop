package com.willfp.ecoshop.shop.gui

import com.willfp.eco.core.config.updating.ConfigUpdater
import com.willfp.eco.core.drops.DropQueue
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.menu.MenuLayer
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.ConfigSlot
import com.willfp.ecoshop.EcoShopPlugin
import com.willfp.ecoshop.shop.isSellable
import com.willfp.ecoshop.shop.sell
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object SellGUI {
    private lateinit var menu: Menu

    @JvmStatic
    @ConfigUpdater
    fun update(plugin: EcoShopPlugin) {
        val rows = plugin.configYml.getInt("sell-gui.rows")

        menu = menu(rows) {
            title = plugin.configYml.getFormattedString("sell-gui.title")

            allowChangingHeldItem()

            for (i in 1..rows) {
                for (j in 1..columns) {
                    addComponent(
                        MenuLayer.LOWER,
                        i, j,
                        slot(ItemStack(Material.AIR)) {
                            setCaptive(true)

                            setCaptiveFilter { player, _, itemStack ->
                                itemStack?.isSellable(player) ?: true
                            }
                        }
                    )
                }
            }

            onClose { event, menu ->
                val player = event.player as Player

                val items = menu.getCaptiveItems(player)

                val unsold = items.sell(player)

                DropQueue(player)
                    .addItems(unsold)
                    .forceTelekinesis()
                    .push()
            }

            for (config in plugin.configYml.getSubsections("sell-gui.custom-slots")) {
                setSlot(
                    config.getInt("row"),
                    config.getInt("column"),
                    ConfigSlot(config)
                )
            }
        }
    }

    fun open(player: Player) {
        menu.open(player)
    }
}
