package com.willfp.ecoshop.logging

import com.willfp.ecoshop.plugin
import org.bukkit.Bukkit
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ShopLogger {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    val logDirectory = File(plugin.dataFolder, "logs/shop")

    @Volatile
    private var currentDay: LocalDate? = null
    private var writer: BufferedWriter? = null

    /**
     * Logs a transaction. Safe to call from the main thread; the actual
     * file write happens asynchronously.
     */
    fun log(player: String, type: String, itemId: String, amount: Int, price: Double) {
        val now = LocalDateTime.now()
        val line = "[${now.format(timestampFormatter)}] $player $type $itemId x$amount for $price"

        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            writeLine(now.toLocalDate(), line)
        })
    }

    @Synchronized
    private fun writeLine(day: LocalDate, line: String) {
        try {
            if (currentDay != day || writer == null) {
                writer?.close()
                logDirectory.mkdirs()
                val file = File(logDirectory, "${day.format(dateFormatter)}.log")
                writer = BufferedWriter(FileWriter(file, true))
                currentDay = day
            }

            writer?.appendLine(line)
            writer?.flush()
        } catch (e: Exception) {
            plugin.logger.warning("[EcoShop] Failed to write shop log entry: ${e.message}")
        }
    }
}
