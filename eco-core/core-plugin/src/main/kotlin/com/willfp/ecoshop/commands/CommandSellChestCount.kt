package com.willfp.ecoshop.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.util.savedDisplayName
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.sellchest.SellChestLimits
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.util.StringUtil

object CommandSellChestCount : Subcommand(
    plugin,
    "sellchestcount",
    "ecoshop.command.sellchestcount",
    false
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(plugin.langYml.getMessage("must-specify-player"))
            return
        }

        @Suppress("DEPRECATION")
        val player = Bukkit.getOfflinePlayer(args[0])
        if (!player.hasPlayedBefore() && !player.isOnline) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-player"))
            return
        }

        if (args.getOrNull(1).equals("set", ignoreCase = true)) {
            val count = args.getOrNull(2)?.toIntOrNull()
            if (count == null || count < 0) {
                sender.sendMessage(plugin.langYml.getMessage("invalid-amount"))
                return
            }
            SellChestLimits.setCount(player, count)
            sender.sendMessage(
                plugin.langYml.getMessage("sellchest.count-set")
                    .replace("%player%", player.savedDisplayName)
                    .replace("%count%", count.toString())
            )
            return
        }

        val limit = player.player?.let { SellChestLimits.limitFor(it) }
        sender.sendMessage(
            plugin.langYml.getMessage("sellchest.count")
                .replace("%player%", player.savedDisplayName)
                .replace("%count%", SellChestLimits.count(player).toString())
                .replace("%limit%", when {
                    limit == null -> "?"
                    limit < 0 -> "∞"
                    else -> limit.toString()
                })
        )
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        val completions = mutableListOf<String>()

        when (args.size) {
            1 -> StringUtil.copyPartialMatches(args[0], Bukkit.getOnlinePlayers().map { it.name }, completions)
            2 -> StringUtil.copyPartialMatches(args[1], listOf("set"), completions)
        }

        return completions
    }
}
