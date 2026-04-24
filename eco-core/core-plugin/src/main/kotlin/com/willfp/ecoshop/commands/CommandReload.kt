package com.willfp.ecoshop.commands

import com.willfp.eco.core.Prerequisite
import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.util.StringUtils
import com.willfp.eco.util.toNiceString
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.shop.ShopCategories
import com.willfp.ecoshop.shop.Shops
import org.bukkit.command.CommandSender

object CommandReload : Subcommand(
    plugin,
    "reload",
    "ecoshop.command.reload",
    false
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        val runnable = Runnable {
            sender.sendMessage(
                plugin.langYml.getMessage("reloaded", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                    .replace("%time%", plugin.reloadWithTime().toNiceString())
                    .replace("%shops%", Shops.values().size.toString())
                    .replace("%categories%", ShopCategories.values().size.toString())
            )
        }
        if (Prerequisite.HAS_FOLIA.isMet)
            plugin.scheduler.runTask(runnable) // run on global thread
        else
            runnable.run()
    }
}
