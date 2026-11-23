package com.willfp.ecoshop.commands

import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.shop.Shop
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import wiki.ecohub.core.command.CommandContext
import wiki.ecohub.core.command.EcoCommand
import wiki.ecohub.core.command.command
import wiki.ecohub.game.entity.runOwned

private const val OTHERS_PERMISSION = "ecoshop.command.open.others"

/**
 * The command that opens [shop]: its menu, or its single category directly.
 * With [OTHERS_PERMISSION], the first argument opens it for that player instead.
 */
internal fun shopCommand(shop: Shop): EcoCommand =
    plugin.command(name = shop.commandName, permission = "ecoshop.open.${shop.id}") {
        executes {
            val target = if (args.isEmpty() || !sender.hasPermission(OTHERS_PERMISSION)) {
                sender as? Player ?: fail("not-player")
            } else {
                Bukkit.getPlayerExact(args[0]) ?: fail("invalid-player")
            }

            target.runOwned { openShop(shop, target) }
        }

        completes {
            if (args.size != 1 || !sender.hasPermission(OTHERS_PERMISSION)) {
                emptyList()
            } else {
                playerNames()
            }
        }
    }

private fun CommandContext<*>.openShop(shop: Shop, player: Player) {
    val menu = shop.menu

    val category = shop.directCategory

    when {
        menu != null -> menu.open(player)
        category != null -> category.openDirect(player, shop)
        else -> reply("invalid-shop-config", "shop" to shop.id)
    }
}
