package com.willfp.ecoshop.commands

import com.willfp.ecoshop.sellchest.SellChestLimits
import com.willfp.ecoshop.util.infiniteDisplay
import org.bukkit.command.CommandSender
import wiki.ecohub.core.command.CommandBuilder
import wiki.ecohub.game.entity.player.PlayerUtils

private val setCompletions = listOf("set")

/**
 * `sellchestcount <player>` shows the count and, for an online player, their limit;
 * `sellchestcount <player> set <count>` sets the count.
 */
internal fun CommandBuilder<CommandSender>.sellChestCountCommand() = subcommand(
    name = "sellchestcount",
    permission = "ecoshop.command.sellchestcount"
) {
    executes {
        val player = knownPlayer(0)
        val name = PlayerUtils.getSavedDisplayName(player)

        if (args.getOrNull(1).equals("set", ignoreCase = true)) {
            val count = args.getOrNull(2)?.toIntOrNull()?.takeIf { it >= 0 } ?: fail("invalid-amount")
            SellChestLimits.setCount(player, count)
            reply("sellchest.count-set", "player" to name, "count" to count)
        } else {
            val limit = player.player?.let { SellChestLimits.limitFor(it) }
            val limitText = when {
                limit == null -> "?"
                limit < 0 -> infiniteDisplay
                else -> limit.toString()
            }

            reply("sellchest.count", "player" to name, "count" to SellChestLimits.count(player), "limit" to limitText)
        }
    }

    completes {
        when (args.size) {
            1 -> playerNames()
            2 -> setCompletions
            else -> emptyList()
        }
    }
}
