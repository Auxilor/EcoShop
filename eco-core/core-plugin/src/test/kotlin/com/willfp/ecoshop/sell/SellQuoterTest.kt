package com.willfp.ecoshop.sell

import com.willfp.ecoshop.fakeStack
import io.mockk.mockk
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SellQuoterTest {
    private val player = mockk<Player>(relaxed = true)
    private val online = Seller.Online(player)

    @BeforeEach
    fun setUp() {
        DynamicPricer.warn = {}
    }

    @AfterEach
    fun tearDown() = DynamicPricer.resetDefaults()

    private fun quoter(map: Map<ItemStack, SellCandidate?>, chunk: Int = 64) =
        SellQuoter(resolve = { map[it] }, chunkSize = { chunk })

    private fun request(vararg stacks: ItemStack, seller: Seller = online, block: SellRequest.() -> SellRequest = { this }) =
        SellRequest(seller, SellSource.COMMAND, ListTarget(stacks.toList())).block()

    @Test
    fun `sells all matching stacks into one line`() {
        val diamond = testCandidate("diamond")
        val a = fakeStack(10); val b = fakeStack(5)
        val quote = quoter(mapOf(a to diamond, b to diamond)).quote(request(a, b))

        assertEquals(1, quote.lines.size)
        assertEquals(15, quote.lines[0].units)
        assertEquals(listOf(SlotTake(0, 10), SlotTake(1, 5)), quote.lines[0].takes)
        assertEquals(15.0, quote.lines[0].priceMultiplier, 1e-9)
        assertEquals(150.0, quote.lines[0].economyValue!!, 1e-9)
    }

    @Test
    fun `player limit enforced across stacks`() {
        val diamond = testCandidate("diamond", sellLimit = 12, playerSells = 4)
        val a = fakeStack(5); val b = fakeStack(5)
        val quote = quoter(mapOf(a to diamond, b to diamond)).quote(request(a, b))

        assertEquals(8, quote.lines[0].units)
        assertEquals(listOf(SlotTake(0, 5), SlotTake(1, 3)), quote.lines[0].takes)
        assertEquals(listOf(UnsoldSlot(1, UnsoldReason.PLAYER_LIMIT)), quote.unsold)
    }

    @Test
    fun `global limit enforced`() {
        val diamond = testCandidate("diamond", globalSellLimit = 3)
        val a = fakeStack(5)
        val quote = quoter(mapOf(a to diamond)).quote(request(a))

        assertEquals(3, quote.lines[0].units)
        assertEquals(UnsoldReason.GLOBAL_LIMIT, quote.unsold.single().reason)
    }

    @Test
    fun `rejection reasons`() {
        val notInShop = fakeStack(1)
        val filtered = fakeStack(1)
        val noPerm = fakeStack(1)
        val noCond = fakeStack(1)
        val cFiltered = testCandidate("cobblestone")
        val cNoPerm = testCandidate("gold", permitted = false)
        val cNoCond = testCandidate("iron", conditionsMet = false)
        val filter = ItemFilter(emptyList(), listOf(ShopItemRule("cobblestone")), emptyList())

        val quote = quoter(mapOf(notInShop to null, filtered to cFiltered, noPerm to cNoPerm, noCond to cNoCond))
            .quote(request(notInShop, filtered, noPerm, noCond) { copy(filter = filter) })

        assertTrue(quote.isEmpty)
        assertEquals(
            listOf(UnsoldReason.NOT_IN_SHOP, UnsoldReason.FILTERED, UnsoldReason.NO_PERMISSION, UnsoldReason.MISSING_REQUIREMENTS),
            quote.unsold.map { it.reason }
        )
    }

    @Test
    fun `permission and condition checks can be disabled`() {
        val c = testCandidate("gold", permitted = false, conditionsMet = false)
        val s = fakeStack(2)
        val quote = quoter(mapOf(s to c)).quote(request(s) { copy(checkPermissions = false, checkConditions = false) })
        assertEquals(2, quote.lines.single().units)
    }

    @Test
    fun `offline seller uses eligibility and skips permission`() {
        val eligible = testCandidate("diamond", permitted = false)
        val ineligible = testCandidate("xp_item")
        val a = fakeStack(2); val b = fakeStack(2)
        val offline = Seller.Offline(mockk<OfflinePlayer>(relaxed = true)) { it.id == "diamond" }

        val quote = quoter(mapOf(a to eligible, b to ineligible)).quote(request(a, b, seller = offline))

        assertEquals("diamond", quote.lines.single().candidate.id)
        assertEquals(UnsoldReason.OFFLINE_INELIGIBLE, quote.unsold.single().reason)
    }

    @Test
    fun `max items caps whole request`() {
        val d = testCandidate("diamond"); val g = testCandidate("gold")
        val a = fakeStack(5); val b = fakeStack(5)
        val quote = quoter(mapOf(a to d, b to g)).quote(request(a, b) { copy(maxItems = 7) })

        assertEquals(listOf(5, 2), quote.lines.map { it.units })
        assertEquals(UnsoldSlot(1, UnsoldReason.CAP_REACHED), quote.unsold.single())
    }

    @Test
    fun `max value caps economy lines only`() {
        val d = testCandidate("diamond", base = 10.0)
        val xp = testCandidate("xp", base = 10.0, economy = false)
        val a = fakeStack(10); val b = fakeStack(10)
        val quote = quoter(mapOf(a to d, b to xp)).quote(request(a, b) { copy(maxValue = 35.0) })

        assertEquals(3, quote.lines[0].units)
        assertEquals(30.0, quote.lines[0].economyValue!!, 1e-9)
        assertEquals(10, quote.lines[1].units)
        assertNull(quote.lines[1].economyValue)
        assertEquals(UnsoldSlot(0, UnsoldReason.CAP_REACHED), quote.unsold.single())
    }

    @Test
    fun `extra and filter multipliers stack`() {
        val d = testCandidate("diamond", base = 10.0)
        val s = fakeStack(2)
        val filter = ItemFilter(emptyList(), emptyList(), listOf(MultiplierRule(ShopItemRule("diamond"), 2.0)))
        val quote = quoter(mapOf(s to d)).quote(request(s) { copy(filter = filter, extraMultiplier = 1.5) })

        assertEquals(6.0, quote.lines.single().priceMultiplier, 1e-9)
        assertEquals(60.0, quote.lines.single().economyValue!!, 1e-9)
    }

    @Test
    fun `bypass skips player and global limits`() {
        val d = testCandidate("diamond", sellLimit = 2, globalSellLimit = 3, playerSells = 2, globalSells = 3)
        val s = fakeStack(10)

        val blocked = quoter(mapOf(s to d)).quote(request(s) { copy(bypass = SellBypass(playerLimits = true)) })
        assertEquals(UnsoldReason.GLOBAL_LIMIT, blocked.unsold.single().reason)

        val quote = quoter(mapOf(s to d)).quote(request(s) { copy(bypass = SellBypass(playerLimits = true, globalLimits = true)) })
        assertEquals(10, quote.lines.single().units)
        assertTrue(quote.unsold.isEmpty())
    }
}
