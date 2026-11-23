package com.willfp.ecoshop.commands

import com.willfp.ecoshop.shop.ShopCategories
import org.bukkit.command.CommandSender
import wiki.ecohub.core.command.CommandBuilder

internal fun CommandBuilder<CommandSender>.rotateCommand() = subcommand(
    name = "rotate",
    permission = "ecoshop.command.rotate"
) {
    executes {
        val id = argument(0, "must-specify-category")
        val scheduler = ShopCategories.getByID(id)?.scheduler

        if (scheduler == null) {
            reply("invalid-category", "category" to id)
        } else {
            scheduler.forceRotate()
            reply("rotate-success", "category" to id)
        }
    }

    completes {
        if (args.size == 1) {
            ShopCategories.values().filter { it.scheduler != null }.map { it.id }
        } else {
            emptyList()
        }
    }
}
