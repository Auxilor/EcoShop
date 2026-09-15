package com.willfp.ecoshop.commands

import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.sell.SellMessages
import com.willfp.ecoshop.sell.SellRequest
import com.willfp.ecoshop.sell.SellSource
import com.willfp.ecoshop.sell.Seller
import com.willfp.ecoshop.sell.Sells
import com.willfp.ecoshop.sell.playerStorageTarget
import org.bukkit.entity.Player

object CommandSellAll: PluginCommand(
    plugin,
    "all",
    "ecoshop.command.sell.all",
    true
) {
    override fun onExecute(player: Player, args: List<String>) {
        val result = Sells.sell(
            SellRequest(Seller.Online(player), SellSource.COMMAND, playerStorageTarget(player))
        )

        if (result.soldUnits == 0) {
            player.sendMessage(plugin.langYml.getMessage("no-sellable"))
            return
        }

        SellMessages.soldMultiple(player, result)
    }
}
