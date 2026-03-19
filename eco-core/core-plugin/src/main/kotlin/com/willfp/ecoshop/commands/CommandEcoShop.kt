package com.willfp.ecoshop.commands

import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.ecoshop.plugin
import org.bukkit.command.CommandSender

object CommandEcoShop : PluginCommand(
    plugin,
    "ecoshop",
    "ecoshop.command.ecoshop",
    false
) {
    init {
        this.addSubcommand(CommandReload)
            .addSubcommand(CommandResetBuys)
            .addSubcommand(CommandResetSells)
    }

    override fun onExecute(sender: CommandSender, args: List<String>) {
        sender.sendMessage(plugin.langYml.getMessage("invalid-command"))
    }
}
