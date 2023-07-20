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
import com.willfp.ecoshop.shop.BuyStatus
import com.willfp.ecoshop.shop.BuyType
import com.willfp.ecoshop.shop.ShopItem
import com.willfp.ecoshop.shop.configKey
import com.willfp.ecoshop.shop.getDisplay
import com.willfp.ecoshop.shop.kickBack
import com.willfp.ecoshop.shop.openAndStack
import com.willfp.ecoshop.shop.parentShop
import org.bukkit.entity.Player
import kotlin.math.min

class BuyMenu(
    val item: ShopItem,
    private val plugin: EcoShopPlugin,
    private val buyType: BuyType
) {
    private val buyMoreMenu = MoreMenu(item, plugin, "buy") { amount, stacks ->
        slot({ player, _ ->
            item.displayItem
                .modify {
                    setAmount(stacks)
                    setDisplayName(
                        plugin.configYml.getString("buy-more.item-name")
                            .replace("%stacks%", stacks.toString())
                    )
                    addLoreLines(
                        plugin.langYml.getStrings("confirm-buy-price")
                            .replaceIn("%price%", item.getBuyPrice(buyType).getDisplay(player, amount))
                    )
                }
        }) {
            onLeftClick { player, _, _, menu ->
                val status = item.getBuyStatus(player, amount, buyType)

                if (status == BuyStatus.ALLOW) {
                    item.buy(
                        player,
                        amount,
                        buyType,
                        shop = menu.parentShop[player]
                    )

                    player.sendMessage(
                        plugin.langYml.getMessage("bought-item-multiple")
                            .replace("%amount%", amount.toString())
                            .replace("%item%", item.displayName)
                            .replace("%price%", item.getBuyPrice(buyType).getDisplay(player, amount))
                    )
                } else {
                    player.sendMessage(
                        plugin.langYml.getMessage("buy-status.${status.configKey}")
                            .replace("%price%", item.getBuyPrice(buyType).getDisplay(player, amount))
                    )
                }
            }
        }
    }

    private val menu = menu(plugin.configYml.getInt("buy-menu.rows")) {
        title = plugin.configYml.getFormattedString("buy-menu.title")
            .replace("%item%", item.displayName)

        allowChangingHeldItem()

        setMask(
            FillerMask(
                MaskItems.fromItemNames(
                    plugin.configYml.getStrings("buy-menu.mask.items")
                ),
                *plugin.configYml.getStrings("buy-menu.mask.pattern").toTypedArray()
            )
        )

        setSlot(
            plugin.configYml.getInt("buy-menu.item.row"),
            plugin.configYml.getInt("buy-menu.item.column"),
            slot { player, menu ->
                item.displayItem.modify {
                    val amount = menu.amountOfItem[player]

                    setAmount(amount)
                    addLoreLines(
                        plugin.langYml.getStrings("confirm-buy-price")
                            .replaceIn("%price%", item.getBuyPrice(buyType).getDisplay(player, amount))
                    )
                }
            }
        )

        if (item.buyMoreEnabled) {
            addComponent(
                plugin.configYml.getInt("buy-menu.buy-more.row"),
                plugin.configYml.getInt("buy-menu.buy-more.column"),
                slot(Items.lookup(plugin.configYml.getString("buy-menu.buy-more.item"))) {
                    onLeftClick { player, _, _, menu ->
                        menu.parentShop[player]?.clickSound?.playTo(player)
                        buyMoreMenu.open(player, menu)
                    }
                }
            )
        }

        for (config in plugin.configYml.getSubsections("buy-menu.add-buttons")) {
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

        for (config in plugin.configYml.getSubsections("buy-menu.set-buttons")) {
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
            plugin.configYml.getInt("buy-menu.cancel.row"),
            plugin.configYml.getInt("buy-menu.cancel.column"),
            slot(Items.lookup(plugin.configYml.getString("buy-menu.cancel.item"))) {
                onLeftClick { player, _, _, menu ->
                    menu.kickBack(player)
                }
            }
        )

        setSlot(
            plugin.configYml.getInt("buy-menu.confirm.row"),
            plugin.configYml.getInt("buy-menu.confirm.column"),
            slot({ player, menu ->
                Items.lookup(plugin.configYml.getString("buy-menu.confirm.item")).modify {
                    addLoreLines(
                        plugin.langYml.getStrings("confirm-buy-price")
                            .replaceIn("%price%", item.getBuyPrice(buyType).getDisplay(player, menu.amountOfItem[player]))
                    )
                }
            }) {
                onLeftClick { player, _, _, menu ->
                    val amount = menu.amountOfItem[player]

                    val status = item.getBuyStatus(player, amount, buyType)

                    if (status == BuyStatus.ALLOW) {
                        item.buy(
                            player,
                            amount,
                            buyType,
                            menu.parentShop[player]
                        )

                        player.sendMessage(
                            plugin.langYml.getMessage("bought-item-multiple")
                                .replace("%amount%", amount.toString())
                                .replace("%item%", item.displayName)
                                .replace("%price%", item.getBuyPrice(buyType).getDisplay(player, amount))
                        )

                        menu.kickBack(player)
                    } else {
                        player.sendMessage(
                            plugin.langYml.getMessage("buy-status.${status.configKey}")
                                .replace("%price%", item.getBuyPrice(buyType).getDisplay(player, amount))
                        )
                    }
                }
            }
        )

        for (config in plugin.configYml.getSubsections("buy-menu.custom-slots")) {
            setSlot(
                config.getInt("row"),
                config.getInt("column"),
                ConfigSlot(config)
            )
        }

        onOpen { player, menu ->
            menu.amountOfItem[player] = item.buyAmount
        }
    }

    fun open(player: Player, previousMenu: Menu) {
        menu.openAndStack(player, previousMenu)
    }
}
