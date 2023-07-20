package com.willfp.ecoshop.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.ecoshop.EcoShopPlugin
import com.willfp.ecoshop.shop.*
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil

class CommandBuy(plugin: EcoPlugin) : PluginCommand(
    plugin,
    "buy",
    "ecoshop.command.buy",
    true
) {
    private fun getMaxPurchaseAmount() = plugin.configYml.getInt("max-purchase-amount")

    override fun onExecute(player: Player, args: List<String>) {
        if (args.isEmpty()) {
            player.sendMessage(plugin.langYml.getMessage("must-specify-item"))
            return
        }

        val itemId = args[0]
        val shopItem = ShopItems.getByID(itemId)

        if (shopItem == null) {
            player.sendMessage(plugin.langYml.getMessage("invalid-item"))
            return
        }

        val buyType = BuyType.NORMAL  // Or get this from the command arguments
        val maxPurchaseAmount = getMaxPurchaseAmount()
        val amount: Int = if (args.size > 1) {
            try {
                args[1].toInt().also {
                    if (it <= 0) throw NumberFormatException()
                }
            } catch (e: NumberFormatException) {
                player.sendMessage(plugin.langYml.getMessage("invalid-amount"))
                return
            }
        } else {
            1  // Default amount
        }

        // Check if the amount exceeds the maximum limit
        if (amount > maxPurchaseAmount) {
            player.sendMessage(plugin.langYml.getMessage("max-purchase-amount-exceeded").replace("%maxAmount%", maxPurchaseAmount.toString()))
            return
        }

        val buyStatus = shopItem.getBuyStatus(player, amount, buyType)
        if (buyStatus != BuyStatus.ALLOW) {
            player.sendMessage(
                plugin.langYml.getMessage("buy-status.${buyStatus.configKey}")
                    .replace("%price%", shopItem.getBuyPrice(buyType).getDisplay(player, amount))
            )
            return
        }

        shopItem.buy(player, amount, buyType)
        if (amount > 1) {
            player.sendMessage(
                plugin.langYml.getMessage("bought-item-multiple")
                    .replace("%amount%", amount.toString())
                    .replace("%item%", shopItem.displayName)
                    .replace("%price%", shopItem.getBuyPrice(buyType).getDisplay(player, amount))
            )
        } else {
            player.sendMessage(
                plugin.langYml.getMessage("bought-item")
                    .replace("%item%", shopItem.displayName)
                    .replace("%price%", shopItem.getBuyPrice(buyType).getDisplay(player, amount))
            )
        }
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        val completions = mutableListOf<String>()

        if (args.isEmpty()) {
            return ShopItems.values().map { it.id }
        }

        if (args.size == 1) {
            StringUtil.copyPartialMatches(
                args[0],
                ShopItems.values().map { it.id },
                completions
            )
        }

        if (args.size == 2) {
            val maxPurchaseAmount = getMaxPurchaseAmount()
            completions.addAll((1..maxPurchaseAmount).map { it.toString() })
        }

        return completions
    }
}
