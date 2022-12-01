package com.willfp.ecoshop.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.ecoshop.shop.sell
import org.bukkit.entity.Player

class CommandSellAll(plugin: EcoPlugin) : PluginCommand(
    plugin,
    "all",
    "ecoshop.command.sell.all",
    true
) {
    override fun onExecute(player: Player, args: List<String>) {
        var didSell = false

        for (i in 0..35) {
            val itemStack = player.inventory.getItem(i)

            if (itemStack?.sell(player) == true) {
                didSell = true
                player.inventory.clear(i)
            }
        }

        if (!didSell) {
            player.sendMessage(plugin.langYml.getMessage("no-sellable"))
        }
    }
}
