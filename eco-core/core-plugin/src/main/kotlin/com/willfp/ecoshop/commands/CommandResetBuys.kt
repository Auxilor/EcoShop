package com.willfp.ecoshop.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.core.data.PlayerProfile
import com.willfp.eco.core.data.forEachSavedProfile
import com.willfp.eco.util.StringUtils
import com.willfp.eco.util.savedDisplayName
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.shop.ShopItem
import com.willfp.ecoshop.shop.ShopItems
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.util.StringUtil

object CommandResetBuys : Subcommand(
    plugin,
    "resetbuys",
    "ecoshop.command.resetbuys",
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

        val player = if (args[0].equals("all", true)) {
            null
        } else {
            @Suppress("DEPRECATION")
            val offlinePlayer = Bukkit.getOfflinePlayer(args[0])

            if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline) {
                sender.sendMessage(plugin.langYml.getMessage("invalid-player"))
                return
            }

            offlinePlayer
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

        if (items.isEmpty()) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-item"))
            return
        }

        if (player == null) {
            resetAllPlayers(sender, items)
            return
        }

        items.forEach { it.resetTimesBought(player) }

        if (items.size == 1) {
            sender.sendMessage(
                plugin.langYml.getMessage("reset-buys", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                    .replace("%player%", player.savedDisplayName)
                    .replace("%item%", items.first().displayName)
            )
            return
        }

        sender.sendMessage(
            plugin.langYml.getMessage("reset-buys-batch", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                .replace("%players%", "1")
                .replace("%items%", items.size.toString())
                .replace("%resets%", items.size.toString())
        )
    }

    private fun resetAllPlayers(sender: CommandSender, items: List<ShopItem>) {
        items.forEach { it.resetGlobalTimesBought() }

        var playerCount = 0

        forEachSavedProfile(plugin, onProgress = { _, total -> playerCount = total }) { uuid ->
            PlayerProfile.load(uuid).let { profile -> items.forEach { it.resetTimesBought(profile) } }
        }.thenRun {
            plugin.scheduler.global().run {
                if (playerCount == 0) {
                    sender.sendMessage(plugin.langYml.getMessage("invalid-player"))
                    return@run
                }

                sender.sendMessage(
                    plugin.langYml.getMessage("reset-buys-batch", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                        .replace("%players%", playerCount.toString())
                        .replace("%items%", items.size.toString())
                        .replace("%resets%", (playerCount * items.size).toString())
                )
            }
        }
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
