package com.willfp.ecoshop.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.util.StringUtils
import com.willfp.eco.util.savedDisplayName
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.shop.ShopItems
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.util.StringUtil

object CommandResetSells : Subcommand(
    plugin,
    "resetsells",
    "ecoshop.command.resetsells",
    false
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(plugin.langYml.getMessage("must-specify-player"))
            return
        }
        if (args.size == 1) {
            sender.sendMessage(plugin.langYml.getMessage("must-specify-item"))
            return
        }

        val players = if (args[0].equals("all", true)) {
            val targets = LinkedHashMap<String, org.bukkit.OfflinePlayer>()

            for (offline in Bukkit.getOfflinePlayers()) {
                if (offline.hasPlayedBefore() || offline.isOnline) {
                    targets[offline.uniqueId.toString()] = offline
                }
            }

            for (online in Bukkit.getOnlinePlayers()) {
                targets[online.uniqueId.toString()] = online
            }

            targets.values.toList()
        } else {
            @Suppress("DEPRECATION")
            val player = Bukkit.getOfflinePlayer(args[0])

            if (!player.hasPlayedBefore() && !player.isOnline) {
                sender.sendMessage(plugin.langYml.getMessage("invalid-player"))
                return
            }

            listOf(player)
        }

        val items = if (args[1].equals("all", true)) {
            ShopItems.values().toList()
        } else {
            val item = ShopItems[args[1]]
            if (item == null) {
                sender.sendMessage(plugin.langYml.getMessage("invalid-item"))
                return
            }

            listOf(item)
        }

        if (players.isEmpty()) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-player"))
            return
        }

        if (items.isEmpty()) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-item"))
            return
        }

        for (player in players) {
            for (item in items) {
                item.resetTimesSold(player)
            }
        }

        if (players.size == 1 && items.size == 1) {
            val player = players.first()
            val item = items.first()

            sender.sendMessage(
                plugin.langYml.getMessage("reset-sells", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                    .replace("%player%", player.savedDisplayName)
                    .replace("%item%", item.displayName)
            )
            return
        }

        sender.sendMessage(
            plugin.langYml.getMessage("reset-sells-batch", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                .replace("%players%", players.size.toString())
                .replace("%items%", items.size.toString())
                .replace("%resets%", (players.size * items.size).toString())
        )
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        val completions = mutableListOf<String>()

        if (args.isEmpty()) {
            return (Bukkit.getOnlinePlayers().map { it.name } + "all").distinct()
        }

        if (args.size == 1) {
            StringUtil.copyPartialMatches(
                args[0],
                Bukkit.getOnlinePlayers().map { it.name } + "all",
                completions
            )
        }

        if (args.size == 2) {
            StringUtil.copyPartialMatches(
                args[1],
                ShopItems.values().map { it.id } + "all",
                completions
            )
        }

        return completions
    }
}


