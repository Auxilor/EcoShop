package com.willfp.ecoshop.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.ecoshop.shop.sell
import com.willfp.ecoshop.shop.shopItem
import org.bukkit.entity.Player

class CommandSellHandall(plugin: EcoPlugin) : PluginCommand(
    plugin,
    "handall",
    "ecoshop.command.sell.handall",
    true
) {
    override fun onExecute(player: Player, args: List<String>) {
        val test = player.inventory.itemInMainHand.shopItem?.item

        if (test == null) {
            player.sendMessage(plugin.langYml.getMessage("not-sellable"))
            return
        }

        for (i in 0..35) {
            val itemStack = player.inventory.getItem(i)

            if (!test.matches(itemStack) || itemStack == null) {
                continue
            }

            if (!itemStack.sell(player)) {
                player.sendMessage("not-sellable")
                return
            } else {
                player.inventory.clear(i)
            }
        }
    }
}
