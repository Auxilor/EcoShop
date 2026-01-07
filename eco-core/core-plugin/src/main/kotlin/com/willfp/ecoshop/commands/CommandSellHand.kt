package com.willfp.ecoshop.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.ecoshop.shop.sell
import com.willfp.ecoshop.shop.shopItem
import org.bukkit.entity.Player

class CommandSellHand(plugin: EcoPlugin) : PluginCommand(
    plugin,
    "hand",
    "ecoshop.command.sell.hand",
    true
) {
    override fun onExecute(player: Player, args: List<String>) {
        val itemStack = player.inventory.itemInMainHand

        if (itemStack.type.isAir || itemStack.amount == 0) {
            player.sendMessage(plugin.langYml.getMessage("not-sellable"))
            return
        }

        val shopItem = itemStack.shopItem

        if (shopItem == null) {
            player.sendMessage(plugin.langYml.getMessage("not-sellable"))
            return
        }

        val isStrict = plugin.configYml.getBoolOrNull("shop-items.sell-strict-match") ?: false

        val matches = if (isStrict) {
            itemStack.isSimilar(shopItem.item!!.item)
        } else {
            shopItem.item!!.matches(itemStack)
        }

        if (!matches || !shopItem.isSellable) {
            player.sendMessage(plugin.langYml.getMessage("not-sellable"))
            return
        }

        val success = itemStack.sell(player)

        if (!success) {
            player.sendMessage(plugin.langYml.getMessage("not-sellable"))
        }
    }
}