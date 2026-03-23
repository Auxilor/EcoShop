package com.willfp.ecoshop.commands

import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.shop.sell
import com.willfp.ecoshop.shop.shopItem
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object CommandSellAll: PluginCommand(
    plugin,
    "all",
    "ecoshop.command.sell.all",
    true
) {
    override fun onExecute(player: Player, args: List<String>) {
        val itemsToSell = mutableMapOf<Int, ItemStack>()

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
                itemsToSell[i] = itemStack
            }
        }

        if (itemsToSell.isEmpty()) {
            player.sendMessage(plugin.langYml.getMessage("no-sellable"))
            return
        }

        val amountBeforeSell = itemsToSell.values.sumOf { it.amount }
        itemsToSell.values.sell(player)

        for ((slot, stack) in itemsToSell) {
            if (stack.type.isAir || stack.amount <= 0) {
                player.inventory.clear(slot)
            } else {
                player.inventory.setItem(slot, stack)
            }
        }

        val amountAfterSell = itemsToSell.values.sumOf { if (it.type.isAir || it.amount <= 0) 0 else it.amount }
        if (amountBeforeSell == amountAfterSell) {
            player.sendMessage(plugin.langYml.getMessage("no-sellable"))
        }
    }
}