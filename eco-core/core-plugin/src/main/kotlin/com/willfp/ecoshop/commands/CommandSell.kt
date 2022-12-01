package com.willfp.ecoshop.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.ecoshop.shop.gui.SellGUI
import org.bukkit.entity.Player

class CommandSell(plugin: EcoPlugin) : PluginCommand(
    plugin,
    "sell",
    "ecoshop.command.sell",
    true
) {
    init {
        this.addSubcommand(CommandSellHand(plugin))
            .addSubcommand(CommandSellHandall(plugin))
            .addSubcommand(CommandSellAll(plugin))
    }

    override fun onExecute(player: Player, args: List<String>) {
        if (args.isEmpty()) {
            SellGUI.open(player)
        } else {
            player.sendMessage(plugin.langYml.getMessage("invalid-sell-type"))
        }
    }
}
