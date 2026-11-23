package com.willfp.ecoshop.commands

import com.willfp.ecoshop.shop.ShopCategories
import com.willfp.ecoshop.shop.Shops
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import wiki.ecohub.core.command.CommandBuilder
import wiki.ecohub.core.platform.runGlobal
import wiki.ecohub.core.stdlib.toNiceString

/**
 * Reloading rebuilds shared registries, so it runs on the global region.
 */
internal fun CommandBuilder<CommandSender>.reloadCommand() = subcommand(
    name = "reload",
    permission = "ecoshop.command.reload"
) {
    executes {
        Bukkit.getServer().runGlobal {
            replyWithoutPlaceholders(
                "reloaded",
                "time" to plugin.reloadWithTime().toNiceString(),
                "shops" to Shops.values().size.toString(),
                "categories" to ShopCategories.values().size.toString()
            )
        }
    }
}
