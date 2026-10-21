package com.willfp.ecoshop.commands

import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.shop.sell
import com.willfp.ecoshop.shop.shopItem
import org.bukkit.entity.Player

object CommandSellHand : PluginCommand(
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

        if (!shopItem.matchesForSale(itemStack) || !shopItem.isSellable) {
            player.sendMessage(plugin.langYml.getMessage("not-sellable"))
            return
        }

        val success = itemStack.sell(player)

        if (!success) {
            player.sendMessage(plugin.langYml.getMessage("not-sellable"))
        }
    }
}