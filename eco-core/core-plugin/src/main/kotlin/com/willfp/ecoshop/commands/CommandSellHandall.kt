package com.willfp.ecoshop.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.ecoshop.shop.sell
import com.willfp.ecoshop.shop.shopItem
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

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

        val items = mutableMapOf<Int, ItemStack>()

        for (i in 0..35) {
            val itemStack = player.inventory.getItem(i)

            if (!test.matches(itemStack) || itemStack == null) {
                continue
            }

            items[i] = itemStack
        }

        val sold = items.values.sell(player)
        if (sold.size == items.size) {
            player.sendMessage("not-sellable")
        } else {
            for (i in items.keys) {
                player.inventory.clear(i)
            }
        }
    }
}
