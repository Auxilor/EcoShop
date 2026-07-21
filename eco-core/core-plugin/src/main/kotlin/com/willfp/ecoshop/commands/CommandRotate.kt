package com.willfp.ecoshop.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.shop.ShopCategories
import org.bukkit.command.CommandSender
import org.bukkit.util.StringUtil

object CommandRotate : Subcommand(
    plugin,
    "rotate",
    "ecoshop.command.rotate",
    false
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(plugin.langYml.getMessage("must-specify-category"))
            return
        }

        val category = ShopCategories.getByID(args[0])
        val scheduler = category?.scheduler

        if (scheduler == null) {
            sender.sendMessage(
                plugin.langYml.getMessage("invalid-category")
                    .replace("%category%", args[0])
            )
            return
        }

        scheduler.forceRotate()
        sender.sendMessage(
            plugin.langYml.getMessage("rotate-success")
                .replace("%category%", args[0])
        )
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        val completions = mutableListOf<String>()
        if (args.size == 1) {
            val rotatingIds = ShopCategories.values()
                .filter { it.scheduler != null }
                .map { it.id }
            StringUtil.copyPartialMatches(args[0], rotatingIds, completions)
        }
        return completions
    }
}
