package com.willfp.ecoshop.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.util.StringUtils
import com.willfp.ecoshop.logging.ShopLogger
import com.willfp.ecoshop.plugin
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.scheduler.BukkitRunnable

object CommandHistory : Subcommand(
    plugin,
    "history",
    "ecoshop.command.history",
    false
) {
    private val validTypes = setOf("buy", "sell", "all")

    override fun onExecute(sender: CommandSender, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(plugin.langYml.getMessage("must-specify-player"))
            return
        }

        val playerName = args[0]

        val amount = args.getOrNull(1)?.toIntOrNull()
        if (amount == null || amount <= 0) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-amount"))
            return
        }

        val type = (args.getOrNull(2) ?: "all").lowercase()
        if (type !in validTypes) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-history-type"))
            return
        }

        val maxDays = plugin.configYml.getInt("logging.max-history-days")

        object : BukkitRunnable() {
            override fun run() {
                val matches = scanHistory(playerName, amount, type, maxDays)

                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (matches.isEmpty()) {
                        sender.sendMessage(
                            plugin.langYml.getMessage("no-history-found", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                                .replace("%player%", playerName)
                        )
                        return@Runnable
                    }

                    sender.sendMessage(
                        plugin.langYml.getMessage("history-header", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                            .replace("%amount%", matches.size.toString())
                            .replace("%player%", playerName)
                    )
                    for (line in matches) {
                        sender.sendMessage(line)
                    }
                })
            }
        }.runTaskAsynchronously(plugin)
    }

    /** Scans log files newest-day-first, returns up to [amount] matching lines. */
    private fun scanHistory(playerName: String, amount: Int, type: String, maxDays: Int): List<String> {
        val results = mutableListOf<String>()

        if (!ShopLogger.logDirectory.exists()) {
            return results
        }

        val files = ShopLogger.logDirectory.listFiles { f -> f.extension == "log" }
            ?.sortedByDescending { it.name }
            ?.take(maxDays)
            ?: return results

        for (file in files) {
            file.bufferedReader().useLines { lines ->
                for (line in lines.asIterable().reversed()) {
                    if (results.size >= amount) return results

                    val parts = line.substringAfter("] ").split(" ")
                    if (parts.size < 2) continue

                    val linePlayer = parts[0]
                    val lineType = parts[1]

                    if (!linePlayer.equals(playerName, ignoreCase = true)) continue
                    if (type != "all" && !lineType.equals(type, ignoreCase = true)) continue

                    results.add(line)
                }
            }

            if (results.size >= amount) break
        }

        return results
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        return when (args.size) {
            1 -> Bukkit.getOnlinePlayers().map { it.name }
            3 -> validTypes.toList()
            else -> emptyList()
        }
    }
}
