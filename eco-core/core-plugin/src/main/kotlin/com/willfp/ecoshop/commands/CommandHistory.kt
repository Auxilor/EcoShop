package com.willfp.ecoshop.commands

import com.willfp.ecoshop.logging.ShopLogger
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import wiki.ecohub.core.command.CommandBuilder
import wiki.ecohub.core.command.CommandContext
import java.io.File

private val validTypes = listOf("buy", "sell", "all")

private val amountCompletions = listOf("1", "5", "10", "25", "50")

private const val TYPE_ARGS = 3

/**
 * `history <player> <amount> [buy|sell|all]`. The logs are read async, newest day and line
 * first; the reply goes back to the sender's region.
 */
internal fun CommandBuilder<CommandSender>.historyCommand() = subcommand(
    name = "history",
    permission = "ecoshop.command.history"
) {
    executes {
        val playerName = argument(0, "must-specify-player")
        val amount = args.getOrNull(1)?.toIntOrNull()?.takeIf { it > 0 } ?: fail("invalid-amount")
        val type = (args.getOrNull(2) ?: "all").lowercase().takeIf { it in validTypes } ?: fail("invalid-history-type")
        val maxDays = plugin.configYml.getInt("logging.max-history-days")

        plugin.scheduler.async().run {
            val matches = scanHistory(playerName, amount, type, maxDays)
            val replyContext = (sender as? Player)?.let { plugin.scheduler.on(it) } ?: plugin.scheduler.global()

            replyContext.run { sendHistory(playerName, matches) }
        }
    }

    completes {
        when (args.size) {
            1 -> playerNames()
            2 -> amountCompletions
            TYPE_ARGS -> validTypes
            else -> emptyList()
        }
    }
}

private fun CommandContext<CommandSender>.sendHistory(playerName: String, matches: List<String>) {
    if (matches.isEmpty()) {
        replyWithoutPlaceholders("no-history-found", "player" to playerName)
    } else {
        replyWithoutPlaceholders("history-header", "amount" to matches.size.toString(), "player" to playerName)
        matches.forEach { sender.sendMessage(it) }
    }
}

private fun scanHistory(playerName: String, amount: Int, type: String, maxDays: Int): List<String> {
    val files = ShopLogger.logDirectory.takeIf { it.exists() }
        ?.run { listFiles { file -> file.extension == "log" } }
        .orEmpty()
        .asSequence()
        .sortedByDescending { it.name }
        .take(maxDays)

    return files
        .flatMap { readNewestFirst(it) }
        .filter { matches(it, playerName, type) }
        .take(amount)
        .toList()
}

private fun readNewestFirst(file: File): Sequence<String> = file.readLines().asReversed().asSequence()

private fun matches(line: String, playerName: String, type: String): Boolean {
    val parts = line.substringAfter("] ").split(" ")

    return parts.size >= 2 &&
        parts[0].equals(playerName, ignoreCase = true) &&
        (type == "all" || parts[1].equals(type, ignoreCase = true))
}
