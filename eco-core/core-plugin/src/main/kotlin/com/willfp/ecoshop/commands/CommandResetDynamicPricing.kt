package com.willfp.ecoshop.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.shop.ShopItems
import org.bukkit.command.CommandSender
import org.bukkit.util.StringUtil

object CommandResetDynamicPricing : Subcommand(
    plugin,
    "resetdynamicpricing",
    "ecoshop.command.resetdynamicpricing",
    false
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(plugin.langYml.getMessage("must-specify-item"))
            return
        }

        val items = if (args[0].equals("all", true)) {
            ShopItems.values().toList()
        } else {
            val item = ShopItems[args[0]]
            if (item == null) {
                sender.sendMessage(plugin.langYml.getMessage("invalid-item"))
                return
            }
            listOf(item)
        }

        for (item in items) {
            item.resetDynamicPricing()
        }

        if (items.size == 1) {
            sender.sendMessage(
                plugin.langYml.getMessage("reset-dynamic-pricing")
                    .replace("%item%", items.first().displayName)
            )
        } else {
            sender.sendMessage(
                plugin.langYml.getMessage("reset-dynamic-pricing-batch")
                    .replace("%items%", items.size.toString())
            )
        }
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        val completions = mutableListOf<String>()

        if (args.size == 1) {
            StringUtil.copyPartialMatches(
                args[0],
                ShopItems.values().map { it.id } + "all",
                completions
            )
        }

        return completions
    }
}
