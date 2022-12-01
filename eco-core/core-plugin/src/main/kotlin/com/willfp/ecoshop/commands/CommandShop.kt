package com.willfp.ecoshop.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.ecoshop.shop.Shop
import com.willfp.ecoshop.shop.ShopCategories
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CommandShop(
    private val shop: Shop,
    plugin: EcoPlugin
) : PluginCommand(
    plugin,
    shop.commandName,
    "ecoshop.open.${shop.id}",
    true
) {
    override fun onExecute(player: Player, args: List<String>) {
        if (shop.menu != null) {
            shop.menu.open(player)
        } else {
            if (shop.directCategory == null) {
                player.sendMessage(plugin.langYml.getMessage("invalid-shop-config").replace("%shop%", shop.id))
                return
            }

            shop.directCategory?.openDirect(player, shop)
        }
    }
}
