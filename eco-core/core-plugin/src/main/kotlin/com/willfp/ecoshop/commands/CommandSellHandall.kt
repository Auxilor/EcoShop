package com.willfp.ecoshop.commands

import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.sell.SellMessages
import com.willfp.ecoshop.sell.SellRequest
import com.willfp.ecoshop.sell.SellSource
import com.willfp.ecoshop.sell.Seller
import com.willfp.ecoshop.sell.Sells
import com.willfp.ecoshop.sell.playerStorageTarget
import com.willfp.ecoshop.shop.shopItem
import org.bukkit.entity.Player

object CommandSellHandall : PluginCommand(
    plugin,
    "handall",
    "ecoshop.command.sell.handall",
    true
) {
    override fun onExecute(player: Player, args: List<String>) {
        val handStack = player.inventory.itemInMainHand

        if (handStack.type.isAir || handStack.amount == 0) {
            player.sendMessage(plugin.langYml.getMessage("not-sellable"))
            return
        }

        val shopItem = handStack.shopItem

        if (shopItem == null) {
            player.sendMessage(plugin.langYml.getMessage("not-sellable"))
            return
        }

        val result = Sells.sell(
            SellRequest(Seller.Online(player), SellSource.COMMAND, playerStorageTarget(player)),
            resolveWith = { stack -> shopItem.takeIf { it.matchesForSale(stack) } }
        )

        if (result.soldUnits == 0) {
            player.sendMessage(plugin.langYml.getMessage("not-sellable"))
            return
        }

        SellMessages.soldMultiple(player, result)
    }
}
