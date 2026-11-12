package com.willfp.ecoshop.shop.gui

import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.sell.SellSource
import com.willfp.ecoshop.shop.ShopItems
import com.willfp.ecoshop.shop.sell
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import wiki.ecohub.game.drop.DropQueue
import wiki.ecohub.modules.menus.addCustomSlots
import wiki.ecohub.modules.menus.menu
import wiki.ecohub.modules.menus.menu.Menu
import wiki.ecohub.modules.menus.menu.MenuLayer
import wiki.ecohub.modules.menus.slot

object SellGUI {
    @Volatile
    private lateinit var menu: Menu

    @JvmStatic
    fun update() {
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
                                if (itemStack == null) return@setCaptiveFilter true

                                ShopItems.values().any { shopItem ->
                                    shopItem.matchesForSale(itemStack) && shopItem.isSellable
                                }
                            }
                        }
                    )
                }
            }

            onClose { event, menu ->
                val player = event.player as Player

                val items = menu.getCaptiveItems(player)

                val unsold = items.sell(player, null, SellSource.GUI)

                DropQueue(player)
                    .addItems(unsold)
                    .forceTelekinesis()
                    .push()
            }

            addCustomSlots(plugin.configYml, "sell-gui")
        }
    }

    fun open(player: Player) {
        menu.open(player)
    }
}
