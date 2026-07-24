package com.willfp.ecoshop.shop.rotation

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.util.NumberUtils
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.shop.ShopItem
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class RotationScheduler(
    private val categoryId: String,
    private val rotationConfig: Config,
    private val rotatingItems: List<ShopItem>,
    private val fixedSlotItems: Map<Int, ShopItem>,
    // r-slots minus static-item-blocked slots.
    private val capacity: Int,
    private val onRotate: (CategoryRotationState) -> Unit
) {
    private val stateDir = File(plugin.dataFolder, "rotation")

    @Volatile
    var state: CategoryRotationState = CategoryRotationState(categoryId, emptyList(), 0L)
        private set

    private var task: BukkitTask? = null

    fun start() {
        state = CategoryRotationState.load(categoryId, stateDir)
        val now = System.currentTimeMillis()
        if (state.nextRotation <= now) {
            rotate()
        } else {
            val delayTicks = ((state.nextRotation - now) / 50).coerceAtLeast(1L)
            scheduleRotation(delayTicks)
        }
    }

    fun forceRotate() {
        task?.cancel()
        rotate()
    }

    fun persist() {
        state.save(stateDir)
    }

    fun stop() {
        task?.cancel()
        task = null
    }

    private fun rotate() {
        val nextMs = computeNextRotationMs()
        val newActiveItems = resolveSlots()

        state = CategoryRotationState(categoryId, newActiveItems, nextMs)
        persist()

        if (rotationConfig.getBool("reset-limits")) {
            resetRotatingItemLimits()
        }

        onRotate(state)

        val delayTicks = ((nextMs - System.currentTimeMillis()) / 50).coerceAtLeast(20L)
        scheduleRotation(delayTicks)
    }

    private fun scheduleRotation(delayTicks: Long) {
        task = Bukkit.getScheduler().runTaskLater(plugin, Runnable { rotate() }, delayTicks)
    }

    private fun computeNextRotationMs(): Long {
        return when {
            rotationConfig.has("rotation-time.interval") ->
                System.currentTimeMillis() + parseIntervalMs(rotationConfig.getString("rotation-time.interval"))
            rotationConfig.has("rotation-time.server-time") ->
                computeNextServerTimeMs(rotationConfig.getString("rotation-time.server-time"))
            else -> {
                // Default to 1h rather than throw, so a misconfigured category still rotates.
                plugin.logger.warning(
                    "Rotation: category '$categoryId' has no rotation-time.interval or " +
                        "rotation-time.server-time — defaulting to 1h."
                )
                System.currentTimeMillis() + 3_600_000L
            }
        }
    }

    private fun parseIntervalMs(interval: String): Long {
        val value = interval.dropLast(1).toLongOrNull()
            ?: throw IllegalArgumentException("Invalid interval value: $interval")
        return when (interval.last()) {
            's' -> value * 1_000L
            'm' -> value * 60_000L
            'h' -> value * 3_600_000L
            'd' -> value * 86_400_000L
            else -> throw IllegalArgumentException("Unknown interval unit in: $interval")
        }
    }

    private fun computeNextServerTimeMs(timeStr: String): Long {
        val target = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"))
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        var next = LocalDateTime.of(LocalDate.now(zone), target)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return next.atZone(zone).toInstant().toEpochMilli()
    }

    // Fixed-slot must be reserved before always-show/weighted, or they'll fill
    // left-to-right and steal a fixed item's reserved index.
    private fun resolveSlots(): List<String> {
        if (capacity <= 0) return emptyList()
        val result = Array(capacity) { "" }
        val available = (0 until capacity).toMutableList()
        val fixedIds = fixedSlotItems.values.map { it.id }.toSet()

        for ((idx, item) in fixedSlotItems) {
            if (idx >= capacity) continue
            result[idx] = item.id
            available.remove(idx)
        }

        val alwaysShow = rotatingItems.filter { it.alwaysShow && it.id !in fixedIds }
        for (item in weightedSample(alwaysShow, available.size)) {
            result[available.removeFirst()] = item.id
        }

        val placed = result.filter { it.isNotEmpty() }.toSet()
        val pool = rotatingItems.filter { !it.alwaysShow && it.id !in placed && it.id !in fixedIds }
        for (item in weightedSample(pool, available.size)) {
            result[available.removeFirst()] = item.id
        }

        return result.toList()
    }

    // Efraimidis-Spirakis weighted sampling without replacement (key = u^(1/w)):
    // distinct items every draw, first pick has probability exactly weight/sum(weight).
    private fun weightedSample(items: List<ShopItem>, count: Int): List<ShopItem> {
        if (count <= 0 || items.isEmpty()) return emptyList()
        return items
            .map { it to Math.pow(NumberUtils.randFloat(0.0, 1.0), 1.0 / it.rotationWeight.coerceAtLeast(1)) }
            .sortedByDescending { it.second }
            .take(count)
            .map { it.first }
    }

    // Online players only - resetting every offline player's profile on the main
    // thread would mean a disk write per player per item, every rotation.
    private fun resetRotatingItemLimits() {
        for (player in Bukkit.getOnlinePlayers()) {
            for (item in rotatingItems) {
                item.resetTimesBought(player)
                item.resetTimesSold(player)
            }
        }
    }
}
