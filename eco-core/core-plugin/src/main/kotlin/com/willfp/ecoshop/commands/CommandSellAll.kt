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
        val toSell = mutableListOf<ItemStack>()
        val slotsToClear = mutableListOf<Int>()

        val isStrict = plugin.configYml.getBoolOrNull("shop-items.sell-strict-match") ?: false

        for (i in 0..35) {
            val itemStack = player.inventory.getItem(i) ?: continue

            val shopItem = itemStack.shopItem ?: continue
            if (!shopItem.isSellable) continue

            val matches = if (isStrict) {
                itemStack.isSimilar(shopItem.item!!.item)
            } else {
                shopItem.item!!.matches(itemStack)
            }

            if (matches) {
                toSell.add(itemStack)
                slotsToClear.add(i)
            }
        }

        if (toSell.isEmpty()) {
            player.sendMessage(plugin.langYml.getMessage("no-sellable"))
            return
        }

        val unsold = toSell.sell(player)

        if (unsold.isEmpty()) {
            for (slot in slotsToClear) {
                player.inventory.clear(slot)
            }
        } else {
            for (slot in slotsToClear) {
                player.inventory.clear(slot)
            }
        }
    }
}