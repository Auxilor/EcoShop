package com.willfp.ecoshop.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.runOnGlobalRegion
import com.willfp.ecoshop.shop.ShopCategories
import com.willfp.ecoshop.shop.Shops
import org.bukkit.command.CommandSender
import wiki.ecohub.core.stdlib.toNiceString
import wiki.ecohub.ui.text.StringUtils

object CommandReload: Subcommand(
    plugin,
    "reload",
    "ecoshop.command.reload",
    false
) {
    override fun onExecute(sender: CommandSender, args: List<String>) = runOnGlobalRegion {
        sender.sendMessage(
            plugin.langYml.getMessage("reloaded", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                .replace("%time%", plugin.reloadWithTime().toNiceString())
                .replace("%shops%", Shops.values().size.toString())
                .replace("%categories%", ShopCategories.values().size.toString())
        )
    }
}
