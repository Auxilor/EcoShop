package com.willfp.ecoshop.shop.gui

import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.onLeftClick
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.ConfigSlot
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.modify
import com.willfp.ecoshop.EcoShopPlugin
import com.willfp.ecoshop.shop.SellStatus
import com.willfp.ecoshop.shop.ShopItem
import com.willfp.ecoshop.shop.configKey
import com.willfp.ecoshop.shop.getDisplay
import com.willfp.ecoshop.shop.kickBack
import com.willfp.ecoshop.shop.openAndStack
import com.willfp.ecoshop.shop.parentShop
import org.bukkit.entity.Player
import kotlin.math.min

class SellMenu(
    val item: ShopItem,
    private val plugin: EcoShopPlugin
) {
    private val sellMoreMenu = MoreMenu(item, plugin, "sell") { amount, stacks ->
        slot({ player, _ ->
            item.displayItem
                .modify {
                    setAmount(stacks)
                    setDisplayName(
                        plugin.configYml.getString("sell-more.item-name")
                            .replace("%stacks%", stacks.toString())
                    )
                    addLoreLines(
                        plugin.langYml.getStrings("confirm-sell-price")
                            .replaceIn("%price%", item.sellPrice.getDisplay(player, amount))
                            .replaceIn("%amount%", amount.toString())
                    )
                }
        }) {
            onLeftClick { player, _, _, menu ->
                val status = item.getCurrentSellStatus(player, amount)

                if (status == SellStatus.ALLOW) {
                    val didSell = item.sell(
                        player,
                        amount,
                        shop = menu.parentShop[player]
                    )

                    player.sendMessage(
                        plugin.langYml.getMessage("sold-item")
                            .replace("%amount%", didSell.toString())
                            .replace("%item%", item.displayName)
                            .replace("%price%", item.sellPrice.getDisplay(player, didSell))
                    )
                } else {
                    player.sendMessage(
                        plugin.langYml.getMessage("sell-status.${status.configKey}")
                    )
                }
            }
        }
    }

    private val menu = menu(plugin.configYml.getInt("sell-menu.rows")) {
        title = plugin.configYml.getFormattedString("sell-menu.title")
            .replace("%item%", item.displayName)

        allowChangingHeldItem()

        setMask(
            FillerMask(
                MaskItems.fromItemNames(
                    plugin.configYml.getStrings("sell-menu.mask.items")
                ),
                *plugin.configYml.getStrings("sell-menu.mask.pattern").toTypedArray()
            )
        )

        setSlot(
            plugin.configYml.getInt("sell-menu.item.row"),
            plugin.configYml.getInt("sell-menu.item.column"),
            slot { player, menu ->
                item.displayItem.modify {
                    val amount = menu.amountOfItem[player]

                    setAmount(amount)
                    addLoreLines(
                        plugin.langYml.getStrings("confirm-sell-price")
                            .replaceIn("%price%", item.sellPrice.getDisplay(player, amount))
                            .replaceIn("%amount%", amount.toString())
                    )
                }
            }
        )

        addComponent(
            plugin.configYml.getInt("sell-menu.sell-more.row"),
            plugin.configYml.getInt("sell-menu.sell-more.column"),
            slot(Items.lookup(plugin.configYml.getString("sell-menu.sell-more.item"))) {
                onLeftClick { player, _, _, menu ->
                    menu.parentShop[player]?.clickSound?.playTo(player)
                    sellMoreMenu.open(player, menu)
                }
            }
        )

        for (config in plugin.configYml.getSubsections("sell-menu.add-buttons")) {
            addComponent(
                config.getInt("row"),
                config.getInt("column"),
                AmountAdjustSelector(
                    item,
                    config.getInt("add"),
                    config
                )
            )
        }

        for (config in plugin.configYml.getSubsections("sell-menu.set-buttons")) {
            addComponent(
                config.getInt("row"),
                config.getInt("column"),
                AmountSetSelector(
                    config
                ) {
                    min(
                        item.getMaxBuysAtOnce(it),
                        item.item?.item?.type?.maxStackSize ?: Int.MAX_VALUE
                    ).coerceAtMost(
                        min(
                            64,
                            config.getInt("set")
                        )
                    )
                }
            )
        }

        setSlot(
            plugin.configYml.getInt("sell-menu.cancel.row"),
            plugin.configYml.getInt("sell-menu.cancel.column"),
            slot(Items.lookup(plugin.configYml.getString("sell-menu.cancel.item"))) {
                onLeftClick { player, _, _, menu ->
                    menu.kickBack(player)
                }
            }
        )

        setSlot(
            plugin.configYml.getInt("sell-menu.confirm.row"),
            plugin.configYml.getInt("sell-menu.confirm.column"),
            slot({ player, menu ->
                Items.lookup(plugin.configYml.getString("sell-menu.confirm.item")).modify {
                    addLoreLines(
                        plugin.langYml.getStrings("confirm-sell-price")
                            .replaceIn("%price%", item.sellPrice.getDisplay(player, menu.amountOfItem[player]))
                            .replaceIn("%amount%", menu.amountOfItem[player].toString())
                    )
                }
            }) {
                onLeftClick { player, _, _, menu ->
                    val amount = menu.amountOfItem[player]

                    val status = item.getCurrentSellStatus(player, amount)

                    if (status == SellStatus.ALLOW) {
                        val didSell = item.sell(
                            player,
                            amount,
                            shop = menu.parentShop[player]
                        )

                        player.sendMessage(
                            plugin.langYml.getMessage("sold-item")
                                .replace("%amount%", didSell.toString())
                                .replace("%item%", item.displayName)
                                .replace("%price%", item.sellPrice.getDisplay(player, amount))
                        )

                        menu.kickBack(player)
                    } else {
                        player.sendMessage(
                            plugin.langYml.getMessage("sell-status.${status.configKey}")
                        )
                    }
                }
            }
        )

        for (config in plugin.configYml.getSubsections("sell-menu.custom-slots")) {
            setSlot(
                config.getInt("row"),
                config.getInt("column"),
                ConfigSlot(config)
            )
        }

        onOpen { player, menu ->
            menu.amountOfItem[player] = 1
        }
    }

    fun open(player: Player, previousMenu: Menu) {
        menu.openAndStack(player, previousMenu)
    }
}
