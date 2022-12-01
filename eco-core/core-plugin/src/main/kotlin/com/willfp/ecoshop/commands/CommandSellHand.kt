package com.willfp.ecoshop.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.ecoshop.shop.sell
import org.bukkit.entity.Player

class CommandSellHand(plugin: EcoPlugin) : PluginCommand(
    plugin,
    "hand",
    "ecoshop.command.sell.hand",
    true
) {
    override fun onExecute(player: Player, args: List<String>) {
        val item = player.inventory.itemInMainHand

        if (!item.sell(player)) {
            player.sendMessage(plugin.langYml.getMessage("not-sellable"))
        }
    }
}
