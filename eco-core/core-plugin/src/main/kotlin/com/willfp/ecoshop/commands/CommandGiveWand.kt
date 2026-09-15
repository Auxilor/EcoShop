package com.willfp.ecoshop.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.core.drops.DropQueue
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.sellwand.SellWands
import com.willfp.ecoshop.sellwand.WandItem
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.util.StringUtil

object CommandGiveWand : Subcommand(
    plugin,
    "givewand",
    "ecoshop.command.givewand",
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

        val wand = SellWands[args.getOrNull(1)]
        if (wand == null) {
            sender.sendMessage(plugin.langYml.getMessage("sellwand.invalid-wand"))
            return
        }

        val amount = args.getOrNull(2)?.toIntOrNull()?.coerceAtLeast(1) ?: 1

        val queue = DropQueue(player).forceTelekinesis()
        repeat(amount) {
            queue.addItem(WandItem.create(wand))
        }
        queue.push()

        sender.sendMessage(
            plugin.langYml.getMessage("sellwand.given")
                .replace("%amount%", amount.toString())
                .replace("%wand%", wand.displayName)
                .replace("%player%", player.name)
        )
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        val completions = mutableListOf<String>()

        when (args.size) {
            1 -> StringUtil.copyPartialMatches(args[0], Bukkit.getOnlinePlayers().map { it.name }, completions)
            2 -> StringUtil.copyPartialMatches(args[1], SellWands.values().map { it.id }, completions)
            3 -> StringUtil.copyPartialMatches(args[2], listOf("1", "8", "16"), completions)
        }

        return completions
    }
}
