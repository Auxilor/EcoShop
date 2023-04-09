package com.willfp.ecoshop.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.util.StringUtils
import com.willfp.eco.util.formatEco
import com.willfp.eco.util.savedDisplayName
import com.willfp.ecoshop.shop.ShopItems
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.util.StringUtil

class CommandResetBuys(plugin: EcoPlugin) : Subcommand(
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
        val itemID = args[1]
        val item = ShopItems.getByID(itemID)
        if (item == null) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-item"))
            return
        }
        val playerName = args[0]
        val searchPhrase = plugin.langYml.getString("messages.all-player-search-phrase")
        val offlinePlayers = Bukkit.getOfflinePlayers()
        val isResettingAllPlayers = playerName.equals(searchPhrase, ignoreCase = true)
        if (isResettingAllPlayers) {
            offlinePlayers.forEach { player ->
                item.resetTimesBought(player)
            }
            val broadcast = plugin.langYml.getStrings("messages.broadcast-all-players-reset-buys").formatEco()
            broadcast.forEach { message ->
                Bukkit.broadcastMessage(message.replace("%item%", item.displayName))
            }
            val message = plugin.langYml.getMessage("command-all-players-reset-buys")
            sender.sendMessage(message)
        } else {
            val player = offlinePlayers.find { it.name?.equals(playerName, ignoreCase = true) == true }
            if (player == null) {
                sender.sendMessage(plugin.langYml.getMessage("invalid-player"))
                return
            }
            item.resetTimesBought(player)
            val message = plugin.langYml.getMessage("reset-buys", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                .replace("%player%", player.savedDisplayName)
                .replace("%item%", item.displayName)
            sender.sendMessage(message)
        }
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        val completions = mutableListOf<String>()
        val searchPhrase = plugin.langYml.getString("messages.all-player-search-phrase")
        val nonNullPlayerNames = Bukkit.getOnlinePlayers().map { it.name }
        when (args.size) {
            // When no arguments, return 'online' player names.
            0 -> {
                return nonNullPlayerNames
            }
            // When 1 argument, return partial matches of 'online' player names (and ALL).
            1 -> {
                StringUtil.copyPartialMatches(args[0], nonNullPlayerNames, completions)
                completions.add(searchPhrase)
            }
            // When 2 arguments, return partial matches of Shop Item Ids.
            2 -> {
                val shopItemIds = ShopItems.values().map { it.id }
                StringUtil.copyPartialMatches(args[1], shopItemIds, completions)
            }
        }
        return completions
    }
}
