package com.willfp.ecoshop.sell

import com.willfp.ecoshop.plugin
import org.bukkit.entity.Player

/** Standard chat feedback for completed sales. */
object SellMessages {
    fun soldMultiple(player: Player, result: SellResult) {
        player.sendMessage(
            plugin.langYml.getMessage("sold-multiple")
                .replace("%amount%", result.soldUnits.toString())
                .replace("%price%", result.display(player))
        )
    }

    fun soldItem(player: Player, displayName: String, result: SellResult) {
        player.sendMessage(
            plugin.langYml.getMessage("sold-item")
                .replace("%amount%", result.soldUnits.toString())
                .replace("%item%", displayName)
                .replace("%price%", result.display(player))
        )
    }
}
