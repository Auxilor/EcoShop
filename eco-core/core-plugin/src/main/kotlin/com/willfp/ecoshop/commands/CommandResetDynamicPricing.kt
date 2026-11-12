package com.willfp.ecoshop.commands

import org.bukkit.command.CommandSender
import wiki.ecohub.core.command.CommandBuilder

internal fun CommandBuilder<CommandSender>.resetDynamicPricingCommand() = subcommand(
    name = "resetdynamicpricing",
    permission = "ecoshop.command.resetdynamicpricing"
) {
    executes {
        val items = shopItems(0)

        for (item in items) {
            item.resetDynamicPricing()
        }

        if (items.size == 1) {
            reply("reset-dynamic-pricing", "item" to items.first().displayName)
        } else {
            reply("reset-dynamic-pricing-batch", "items" to items.size)
        }
    }

    completes {
        if (args.size == 1) shopItemIdsAndAll() else emptyList()
    }
}
