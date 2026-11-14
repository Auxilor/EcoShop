package com.willfp.ecoshop.commands

import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.shop.Shop
import wiki.ecohub.core.command.EcoCommand
import wiki.ecohub.core.command.playerCommand

/**
 * The command that opens [shop]: its menu, or its single category directly.
 */
internal fun shopCommand(shop: Shop): EcoCommand =
    plugin.playerCommand(name = shop.commandName, permission = "ecoshop.open.${shop.id}") {
        executes {
            val menu = shop.menu

            val category = shop.directCategory

            when {
                menu != null -> menu.open(sender)
                category != null -> category.openDirect(sender, shop)
                else -> reply("invalid-shop-config", "shop" to shop.id)
            }
        }
    }
