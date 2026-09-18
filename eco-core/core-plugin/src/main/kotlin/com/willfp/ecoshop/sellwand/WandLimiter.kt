package com.willfp.ecoshop.sellwand

import org.bukkit.OfflinePlayer
import java.util.UUID

data class PeriodState(val uses: Int, val startSeconds: Int)

interface PeriodStore {
    fun read(player: OfflinePlayer, wandId: String): PeriodState

    fun write(player: OfflinePlayer, wandId: String, state: PeriodState)
}

sealed interface LimitCheck {
    object Allowed : LimitCheck

    data class OnCooldown(val remainingMillis: Long) : LimitCheck

    data class PeriodExhausted(val resetsInSeconds: Int) : LimitCheck
}

/** Cooldowns (in memory) and period limits (persistent) per player per wand type. */
class WandLimiter(
    private val store: PeriodStore,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val lastUse = HashMap<UUID, HashMap<String, Long>>()

    fun check(player: OfflinePlayer, limits: WandLimits): LimitCheck {
        val now = clock()

        if (limits.cooldownSeconds > 0) {
            val last = lastUse[player.uniqueId]?.get(limits.id)
            val cooldownMillis = limits.cooldownSeconds * 1000L
            if (last != null && now - last < cooldownMillis) {
                return LimitCheck.OnCooldown(cooldownMillis - (now - last))
            }
        }

        if (limits.periodUses >= 0) {
            val state = currentPeriod(player, limits, now)
            if (state.uses >= limits.periodUses) {
                val nowSeconds = (now / 1000L).toInt()
                return LimitCheck.PeriodExhausted(state.startSeconds + limits.periodMinutes * 60 - nowSeconds)
            }
        }

        return LimitCheck.Allowed
    }

    fun recordUse(player: OfflinePlayer, limits: WandLimits) {
        val now = clock()
        lastUse.getOrPut(player.uniqueId) { HashMap() }[limits.id] = now

        if (limits.periodUses >= 0) {
            val state = currentPeriod(player, limits, now)
            val nowSeconds = (now / 1000L).toInt()
            val start = if (state.uses == 0) nowSeconds else state.startSeconds
            store.write(player, limits.id, PeriodState(state.uses + 1, start))
        }
    }

    fun periodUses(player: OfflinePlayer, limits: WandLimits): Int =
        currentPeriod(player, limits, clock()).uses

    /** The stored period, or an empty one if it never started or has expired. */
    private fun currentPeriod(player: OfflinePlayer, limits: WandLimits, now: Long): PeriodState {
        val state = store.read(player, limits.id)
        val nowSeconds = (now / 1000L).toInt()
        if (state.startSeconds == 0 || nowSeconds - state.startSeconds >= limits.periodMinutes * 60) {
            return PeriodState(0, 0)
        }
        return state
    }
}
