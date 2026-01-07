package com.willfp.ecoshop.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.ecoshop.shop.sell
import com.willfp.ecoshop.shop.shopItem
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class CommandSellHandall(plugin: EcoPlugin) : PluginCommand(
    plugin,
    "handall",
    "ecoshop.command.sell.handall",
    true
) {
    override fun onExecute(player: Player, args: List<String>) {
        val handStack = player.inventory.itemInMainHand

        if (handStack.type.isAir || handStack.amount == 0) {
            player.sendMessage(plugin.langYml.getMessage("not-sellable"))
            return
        }

        val shopItem = handStack.shopItem

        if (shopItem == null) {
            player.sendMessage(plugin.langYml.getMessage("not-sellable"))
            return
        }

        val isStrict = plugin.configYml.getBoolOrNull("shop-items.sell-strict-match") ?: false

        val baseItem = shopItem.item!!.item 

        val items = mutableMapOf<Int, ItemStack>()

        for (i in 0..35) {
            val itemStack = player.inventory.getItem(i) ?: continue

            val matches = if (isStrict) {
                itemStack.isSimilar(baseItem)
            } else {
                shopItem.item!!.matches(itemStack)
            }

            if (matches) {
                items[i] = itemStack
            }
        }

        if (items.isEmpty()) {
            player.sendMessage(plugin.langYml.getMessage("no-sellable"))
            return
        }

        val unsold = items.values.sell(player)

        if (unsold.isEmpty()) {
            for (i in items.keys) {
                player.inventory.clear(i)
            }
        } else {
            for (i in items.keys) {
                player.inventory.clear(i)
            }
        }
    }
}
