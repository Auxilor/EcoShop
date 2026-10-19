package com.willfp.ecoshop.shop.gui

import com.willfp.eco.core.gui.addCustomSlots
import com.willfp.eco.core.gui.component.ConfigButton
import com.willfp.eco.core.gui.component.addComponent
import com.willfp.eco.core.gui.getSlotLocations
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.onLeftClick
import com.willfp.eco.core.gui.setMask
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.modify
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.sell.SellMessages
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
    val item: ShopItem
) {
    private val sellMoreMenu = MoreMenu(item, "sell") { amount, stacks ->
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
                            .replaceIn("%price%", item.sellPrice.getDisplay(player, amount * item.getEffectiveSellMultiplier(player)))
                            .replaceIn("%amount%", amount.toString())
                    )
                }
        }) {
            onLeftClick { player, _, _, menu ->
                val status = item.getCurrentSellStatus(player, amount)

                if (status == SellStatus.ALLOW) {
                    val result = item.sellDetailed(
                        player,
                        amount,
                        shop = menu.parentShop[player]
                    )

                    SellMessages.soldItem(player, item.displayName, result)
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

        setMask(plugin.configYml, "sell-menu")

        for (location in plugin.configYml.getSlotLocations("sell-menu.item")) {
            setSlot(
                location.row,
                location.column,
                slot { player, menu ->
                    item.displayItem.modify {
                        val amount = menu.amountOfItem[player]

                        setAmount(amount)

                        setDisplayName(item.displayName)

                        addLoreLines(
                            plugin.langYml.getStrings("confirm-sell-price")
                                .replaceIn("%price%", item.sellPrice.getDisplay(player, amount * item.getEffectiveSellMultiplier(player)))
                                .replaceIn("%amount%", amount.toString())
                        )
                    }
                }
            )
        }

        for (location in plugin.configYml.getSlotLocations("sell-menu.sell-more")) {
            addComponent(
                location.row,
                location.column,
                slot(Items.lookup(plugin.configYml.getString("sell-menu.sell-more.item"))) {
                    onLeftClick { player, _, _, menu ->
                        menu.parentShop[player]?.clickSound?.playTo(player)
                        sellMoreMenu.open(player, menu)
                    }
                }
            )
        }

        for (config in plugin.configYml.getSubsections("sell-menu.add-buttons")) {
            for (location in config.getSlotLocations()) {
                addComponent(
                    location.row,
                    location.column,
                    AmountAdjustSelector(
                        item,
                        config.getInt("add"),
                        config
                    )
                )
            }
        }

        for (config in plugin.configYml.getSubsections("sell-menu.set-buttons")) {
            for (location in config.getSlotLocations()) {
                addComponent(
                    location.row,
                    location.column,
                    AmountSetSelector(
                        config
                    ) {
                        val maxSellable = min(
                            item.getAmountInPlayerInventory(it),
                            min(
                                item.getSellsLeft(it),
                                item.globalSellLimit - item.getTotalGlobalSells()
                            )
                        )

                        min(
                            maxSellable,
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
        }

        addComponent(
            ConfigButton(plugin.configYml.getSubsection("sell-menu.cancel")) { player, _, _, menu ->
                menu.kickBack(player)
            }
        )

        for (location in plugin.configYml.getSlotLocations("sell-menu.confirm")) {
            setSlot(
                location.row,
                location.column,
                slot({ player, menu ->
                    Items.lookup(plugin.configYml.getString("sell-menu.confirm.item")).modify {
                        addLoreLines(
                            plugin.langYml.getStrings("confirm-sell-price")
                                .replaceIn("%price%", item.sellPrice.getDisplay(player, menu.amountOfItem[player] * item.getEffectiveSellMultiplier(player)))
                                .replaceIn("%amount%", menu.amountOfItem[player].toString())
                        )
                    }
                }) {
                    onLeftClick { player, _, _, menu ->
                        val amount = menu.amountOfItem[player]

                        val status = item.getCurrentSellStatus(player, amount)

                        if (status == SellStatus.ALLOW) {
                            val result = item.sellDetailed(
                                player,
                                amount,
                                shop = menu.parentShop[player]
                            )

                            SellMessages.soldItem(player, item.displayName, result)

                            menu.kickBack(player)
                        } else {
                            player.sendMessage(
                                plugin.langYml.getMessage("sell-status.${status.configKey}")
                            )
                        }
                    }
                }
            )
        }

        val sellAllConfig = plugin.configYml.getSubsection("sell-menu.sell-all-button")
        if (sellAllConfig.getBool("enabled")) {
            for (location in sellAllConfig.getSlotLocations()) {
                setSlot(
                    location.row,
                    location.column,
                    slot({ player, _ ->
                        val maxAmount = item.getAmountInPlayerInventory(player)
                        val priceDisplay = item.sellPrice?.getDisplay(player, maxAmount * item.getEffectiveSellMultiplier(player)) ?: "0"

                        val baseItem = Items.lookup(sellAllConfig.getString("item"))

                        baseItem.modify {
                            addLoreLines(
                                sellAllConfig.getStrings("lore").map { line ->
                                    line.replace("%amount%", maxAmount.toString())
                                        .replace("%price%", priceDisplay)
                                }
                            )
                        }
                    }) {
                        onLeftClick { player, _, _, menu ->
                            val maxAmount = item.getAmountInPlayerInventory(player)
                            val status = item.getCurrentSellStatus(player)

                            if (status != SellStatus.ALLOW) {
                                player.sendMessage(plugin.langYml.getMessage("sell-status.${status.configKey}"))
                                return@onLeftClick
                            }

                            if (maxAmount <= 0) {
                                player.sendMessage(plugin.langYml.getMessage("no-sellable"))
                                return@onLeftClick
                            }

                            val result = item.sellDetailed(player, maxAmount, shop = menu.parentShop[player])

                            if (result.soldUnits > 0) {
                                SellMessages.soldItem(player, item.displayName, result)
                                menu.kickBack(player)
                            } else {
                                player.sendMessage(plugin.langYml.getMessage("not-enough"))
                            }
                        }
                    }
                )
            }
        }

        addCustomSlots(plugin.configYml, "sell-menu")

        onOpen { player, menu ->
            menu.amountOfItem[player] = 1
        }
    }

    fun open(player: Player, previousMenu: Menu) {
        menu.openAndStack(player, previousMenu)
    }
}
