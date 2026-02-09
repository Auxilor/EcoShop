package com.willfp.ecoshop.commands

import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.shop.gui.SellGUI
import org.bukkit.entity.Player

object CommandSell : PluginCommand(
    plugin,
    "sell",
    "ecoshop.command.sell",
    true
) {
    init {
        this.addSubcommand(CommandSellHand)
            .addSubcommand(CommandSellHandall)
            .addSubcommand(CommandSellAll)
    }

    override fun onExecute(player: Player, args: List<String>) {
        if (args.isEmpty()) {
            SellGUI.open(player)
        } else {
            player.sendMessage(plugin.langYml.getMessage("invalid-sell-type"))
        }
    }
}
