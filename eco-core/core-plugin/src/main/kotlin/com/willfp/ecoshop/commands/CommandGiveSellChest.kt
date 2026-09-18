package com.willfp.ecoshop.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.core.drops.DropQueue
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.sellchest.SellChestItem
import com.willfp.ecoshop.sellchest.SellChestTypes
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.util.StringUtil

object CommandGiveSellChest : Subcommand(
    plugin,
    "givesellchest",
    "ecoshop.command.givesellchest",
    false
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(plugin.langYml.getMessage("must-specify-player"))
            return
        }

        val player = Bukkit.getPlayer(args[0])
        if (player == null) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-player"))
            return
        }

        val type = SellChestTypes[args.getOrNull(1)]
        if (type == null) {
            sender.sendMessage(plugin.langYml.getMessage("sellchest.invalid-type"))
            return
        }

        val amount = args.getOrNull(2)?.toIntOrNull()?.coerceAtLeast(1) ?: 1

        val queue = DropQueue(player).forceTelekinesis()
        var left = amount
        while (left > 0) {
            val stack = SellChestItem.create(type)
            stack.amount = left.coerceAtMost(stack.maxStackSize)
            left -= stack.amount
            queue.addItem(stack)
        }
        queue.push()

        sender.sendMessage(
            plugin.langYml.getMessage("sellchest.given")
                .replace("%amount%", amount.toString())
                .replace("%type%", type.displayName)
                .replace("%player%", player.name)
        )
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        val completions = mutableListOf<String>()

        when (args.size) {
            1 -> StringUtil.copyPartialMatches(args[0], Bukkit.getOnlinePlayers().map { it.name }, completions)
            2 -> StringUtil.copyPartialMatches(args[1], SellChestTypes.values().map { it.id }, completions)
            3 -> StringUtil.copyPartialMatches(args[2], listOf("1", "8", "16"), completions)
        }

        return completions
    }
}
