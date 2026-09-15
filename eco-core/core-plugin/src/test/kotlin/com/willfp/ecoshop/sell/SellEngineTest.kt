package com.willfp.ecoshop.sell

import com.willfp.ecoshop.event.EcoShopContainerSellEvent
import com.willfp.ecoshop.fakeStack
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SellEngineTest {
    private val events = mutableListOf<Event>()
    private val logs = mutableListOf<SellLogEntry>()
    private var offlinePayResult = true

    private val engine = SellEngine(
        callEvent = { events += it },
        payOffline = { _, _ -> offlinePayResult },
        log = { logs += it }
    )

    private fun line(c: SellCandidate, vararg takes: SlotTake, mult: Double, econ: Double?) =
        QuoteLine(c, takes.toList(), takes.sumOf { it.amount }, 10.0, mult, econ)

    @Test
    fun `online commit applies event multiplier, removes, records, triggers`() {
        val player = mockk<Player>(relaxed = true) { every { name } returns "Steve" }
        val c = testCandidate("diamond")
        every { c.fireSellEvent(player, any(), 5, SellSource.COMMAND) } returns 2.0
        val stack = fakeStack(5)
        val request = SellRequest(Seller.Online(player), SellSource.COMMAND, ListTarget(listOf(stack)))
        val quote = SellQuote(request, listOf(line(c, SlotTake(0, 5), mult = 5.0, econ = 50.0)), emptyList())

        val result = engine.commit(quote)

        verify { c.giveSellPayout(player, 10.0) }
        verify { c.recordSell(player, 5, SellBypass.NONE) }
        verify { c.triggerSellEffects(player, 5) }
        assertEquals(0, stack.amount)
        assertEquals(5, result.soldUnits)
        assertEquals(100.0, result.economyTotal, 1e-9)
        assertEquals("diamond", logs.single().itemId)
    }

    @Test
    fun `bypass is passed to recordSell`() {
        val player = mockk<Player>(relaxed = true)
        val c = testCandidate("diamond")
        val bypass = SellBypass(dynamicPricing = true, playerLimits = true)
        val request = SellRequest(Seller.Online(player), SellSource.WAND, ListTarget(listOf(fakeStack(3))), bypass = bypass)
        val quote = SellQuote(request, listOf(line(c, SlotTake(0, 3), mult = 3.0, econ = 30.0)), emptyList())

        engine.commit(quote)

        verify { c.recordSell(player, 3, bypass) }
    }

    @Test
    fun `event multiplier ignored when disabled`() {
        val player = mockk<Player>(relaxed = true)
        val c = testCandidate("diamond")
        every { c.fireSellEvent(any(), any(), any(), any()) } returns 3.0
        val request = SellRequest(Seller.Online(player), SellSource.CHEST, ListTarget(listOf(fakeStack(1))), applyEventMultiplier = false)
        val quote = SellQuote(request, listOf(line(c, SlotTake(0, 1), mult = 1.0, econ = 10.0)), emptyList())

        engine.commit(quote, mockk<Location>(relaxed = true))

        verify { c.giveSellPayout(player, 1.0) }
    }

    @Test
    fun `offline commit pays economy value and skips player hooks`() {
        val owner = mockk<OfflinePlayer>(relaxed = true) { every { name } returns "Alex" }
        val c = testCandidate("diamond")
        val stack = fakeStack(4)
        val request = SellRequest(Seller.Offline(owner) { true }, SellSource.CHEST, ListTarget(listOf(stack)), applyEventMultiplier = false)
        val quote = SellQuote(request, listOf(line(c, SlotTake(0, 4), mult = 4.0, econ = 40.0)), emptyList())

        val result = engine.commit(quote, mockk(relaxed = true))

        verify(exactly = 0) { c.giveSellPayout(any(), any()) }
        verify(exactly = 0) { c.triggerSellEffects(any(), any()) }
        verify { c.recordSell(owner, 4, SellBypass.NONE) }
        assertEquals(0, stack.amount)
        assertEquals(40.0, result.economyTotal, 1e-9)
    }

    @Test
    fun `failed offline payment keeps items`() {
        offlinePayResult = false
        val owner = mockk<OfflinePlayer>(relaxed = true)
        val c = testCandidate("diamond")
        val stack = fakeStack(4)
        val request = SellRequest(Seller.Offline(owner) { true }, SellSource.CHEST, ListTarget(listOf(stack)))
        val quote = SellQuote(request, listOf(line(c, SlotTake(0, 4), mult = 4.0, econ = 40.0)), emptyList())

        val result = engine.commit(quote, mockk(relaxed = true))

        assertEquals(4, stack.amount)
        assertEquals(1, result.failed.size)
        verify(exactly = 0) { c.recordSell(any(), any(), any()) }
    }

    @Test
    fun `changed slot skips line`() {
        val player = mockk<Player>(relaxed = true)
        val c = testCandidate("diamond")
        val stack = fakeStack(2) // quote expected 5
        val request = SellRequest(Seller.Online(player), SellSource.COMMAND, ListTarget(listOf(stack)))
        val quote = SellQuote(request, listOf(line(c, SlotTake(0, 5), mult = 5.0, econ = 50.0)), emptyList())

        val result = engine.commit(quote)

        assertEquals(2, stack.amount)
        assertEquals(1, result.failed.size)
    }

    @Test
    fun `cancelled container event aborts commit`() {
        val player = mockk<Player>(relaxed = true)
        val c = testCandidate("diamond")
        val stack = fakeStack(1)
        val cancelling = SellEngine(
            callEvent = { if (it is EcoShopContainerSellEvent) it.isCancelled = true },
            payOffline = { _, _ -> true },
            log = {}
        )
        val request = SellRequest(Seller.Online(player), SellSource.WAND, ListTarget(listOf(stack)))
        val quote = SellQuote(request, listOf(line(c, SlotTake(0, 1), mult = 1.0, econ = 10.0)), emptyList())

        val result = cancelling.commit(quote, mockk(relaxed = true))

        assertEquals(1, stack.amount)
        assertTrue(result.paid.isEmpty())
    }
}
