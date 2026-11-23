package com.willfp.ecoshop.commands

import com.willfp.ecoshop.EcoShopPlugin
import com.willfp.ecoshop.sell.SellMessages
import com.willfp.ecoshop.sell.SellRequest
import com.willfp.ecoshop.sell.SellSource
import com.willfp.ecoshop.sell.Seller
import com.willfp.ecoshop.sell.Sells
import com.willfp.ecoshop.sell.playerStorageTarget
import com.willfp.ecoshop.shop.ShopItem
import com.willfp.ecoshop.shop.gui.SellGUI
import com.willfp.ecoshop.shop.sell
import com.willfp.ecoshop.shop.shopItem
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import wiki.ecohub.core.command.CommandContext
import wiki.ecohub.core.command.playerCommand

/**
 * `/sell` opens the sell GUI; `hand`, `handall` and `all` sell from the inventory.
 */
internal fun EcoShopPlugin.sellCommand() = playerCommand(name = "sell", permission = "ecoshop.command.sell") {
    subcommand(name = "hand", permission = "ecoshop.command.sell.hand") {
        executes {
            val itemStack = sender.inventory.itemInMainHand
            val shopItem = heldShopItem(itemStack)?.takeIf { it.matchesForSale(itemStack) && it.isSellable }

            if (shopItem == null || !itemStack.sell(sender)) {
                fail("not-sellable")
            }
        }
    }

    subcommand(name = "handall", permission = "ecoshop.command.sell.handall") {
        executes {
            val shopItem = heldShopItem(sender.inventory.itemInMainHand) ?: fail("not-sellable")
            sellMatching("not-sellable") { stack -> shopItem.takeIf { it.matchesForSale(stack) } }
        }
    }

    subcommand(name = "all", permission = "ecoshop.command.sell.all") {
        executes {
            sellMatching("no-sellable", null)
        }
    }

    executes {
        if (args.isEmpty()) {
            SellGUI.open(sender)
        } else {
            fail("invalid-sell-type")
        }
    }
}

private fun heldShopItem(itemStack: ItemStack): ShopItem? =
    itemStack.takeUnless { it.type.isAir || it.amount == 0 }?.shopItem

private fun CommandContext<Player>.sellMatching(emptyKey: String, resolveWith: ((ItemStack) -> ShopItem?)?) {
    val request = SellRequest(Seller.Online(sender), SellSource.COMMAND, playerStorageTarget(sender))
    val result = Sells.sell(request, resolveWith = resolveWith)

    if (result.soldUnits == 0) {
        fail(emptyKey)
    }

    SellMessages.soldMultiple(sender, result)
}
