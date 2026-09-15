package com.willfp.ecoshop.sellwand

import io.mockk.every
import io.mockk.mockk
import org.bukkit.OfflinePlayer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class WandLimiterTest {
    private var now = 1_000_000_000L
    private val states = HashMap<String, PeriodState>()
    private val store = object : PeriodStore {
        override fun read(player: OfflinePlayer, wandId: String) = states[wandId] ?: PeriodState(0, 0)
        override fun write(player: OfflinePlayer, wandId: String, state: PeriodState) {
            states[wandId] = state
        }
    }
    private val limiter = WandLimiter(store) { now }
    private val player = mockk<OfflinePlayer> { every { uniqueId } returns UUID.randomUUID() }

    @Test
    fun `no limits always allowed`() {
        val limits = WandLimits("w", 0, -1, 1440)
        limiter.recordUse(player, limits)
        assertEquals(LimitCheck.Allowed, limiter.check(player, limits))
    }

    @Test
    fun `cooldown blocks until elapsed`() {
        val limits = WandLimits("w", 5, -1, 1440)
        limiter.recordUse(player, limits)
        now += 2_000
        assertEquals(LimitCheck.OnCooldown(3_000), limiter.check(player, limits))
        now += 3_000
        assertEquals(LimitCheck.Allowed, limiter.check(player, limits))
    }

    @Test
    fun `cooldown is per wand id`() {
        limiter.recordUse(player, WandLimits("a", 5, -1, 1440))
        assertEquals(LimitCheck.Allowed, limiter.check(player, WandLimits("b", 5, -1, 1440)))
    }

    @Test
    fun `period limit exhausts then resets`() {
        val limits = WandLimits("w", 0, 2, 60)
        limiter.recordUse(player, limits)
        limiter.recordUse(player, limits)
        now += 10 * 60_000L
        assertEquals(LimitCheck.PeriodExhausted(50 * 60), limiter.check(player, limits))
        now += 50 * 60_000L
        assertEquals(LimitCheck.Allowed, limiter.check(player, limits))
        limiter.recordUse(player, limits)
        assertEquals(1, limiter.periodUses(player, limits))
    }
}
