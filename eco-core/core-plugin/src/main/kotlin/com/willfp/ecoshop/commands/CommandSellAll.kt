package com.willfp.ecoshop.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.ecoshop.shop.isSellable
import com.willfp.ecoshop.shop.sell
import com.willfp.ecoshop.shop.shopItem
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class CommandSellAll(plugin: EcoPlugin) : PluginCommand(
    plugin,
    "all",
    "ecoshop.command.sell.all",
    true
) {
    override fun onExecute(player: Player, args: List<String>) {
        val items = mutableMapOf<Int, ItemStack>()

        for (i in 0..35) {
            val itemStack = player.inventory.getItem(i) ?: continue

            if (!itemStack.isSellable(player)) {
                continue
            }

            items[i] = itemStack
        }

        val sold = items.values.sell(player)
        if (sold.size == items.size) {
            player.sendMessage(plugin.langYml.getMessage("no-sellable"))
        } else {
            for (i in items.keys) {
                player.inventory.clear(i)
            }
        }
    }
}
