package com.willfp.ecoshop.commands

import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.runOwned
import com.willfp.ecoshop.shop.Shop
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil

class CommandShop(
    private val shop: Shop
) : PluginCommand(
    plugin,
    shop.commandName,
    "ecoshop.open.${shop.id}",
    false
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        val target = findTarget(sender, args) ?: return

        target.runOwned { openShop(target, sender) }
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        if (args.size != 1 || !sender.hasPermission(OTHERS_PERMISSION)) {
            return emptyList()
        }

        return StringUtil.copyPartialMatches(args[0], Bukkit.getOnlinePlayers().map { it.name }, mutableListOf())
    }

    private fun findTarget(sender: CommandSender, args: List<String>): Player? {
        if (args.isEmpty() || !sender.hasPermission(OTHERS_PERMISSION)) {
            if (sender !is Player) {
                sender.sendMessage(plugin.langYml.getMessage("not-player"))
            }

            return sender as? Player
        }

        val target = Bukkit.getPlayerExact(args[0])
        if (target == null) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-player"))
        }

        return target
    }

    private fun openShop(player: Player, sender: CommandSender) {
        if (shop.menu != null) {
            shop.menu.open(player)
            return
        }

        val category = shop.directCategory
        if (category == null) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-shop-config").replace("%shop%", shop.id))
            return
        }

        category.openDirect(player, shop)
    }

    private companion object {
        const val OTHERS_PERMISSION = "ecoshop.command.open.others"
    }
}
