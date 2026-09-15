package com.senspark.client.explodev6

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Client target cache: batching and backoffs, run against the real server code.
class ClientBombTargetManagerTest {

    private lateinit var bed: ServerTestBed
    private lateinit var client: FakeGameClient
    private lateinit var clock: VirtualClock
    private lateinit var targets: ClientBombTargetManager

    private fun boot(map: com.senspark.game.controller.MapData = TestMaps.withBricks(cell(6, 0), cell(10, 4))) {
        bed = ServerTestBed(map)
        bed.disableMoveSpeedCheck()
        client = FakeGameClient(bed)
        clock = VirtualClock()
        targets = ClientBombTargetManager(client) { clock.nowMs() }
    }

    @BeforeEach
    fun setUp() = boot()

    @Test
    fun `every hero queued in one frame goes out as a single batched request`() {
        for (id in 1..5) bed.addHero(id)
        for (id in 1..5) targets.requestTarget(id, cell(0, 0))

        assertTrue(targets.flush())

        assertEquals(1, targets.sentBatches.size, "the batch was not coalesced")
        assertEquals(5, targets.sentBatches.single().size)
        for (id in 1..5) assertNotNull(targets.tryGetTarget(id))
    }

    @Test
    fun `a hero that already holds a target does not ask again`() {
        bed.addHero(1)
        targets.requestTarget(1, cell(0, 0))
        targets.flush()

        targets.requestTarget(1, cell(0, 0))
        assertFalse(targets.flush(), "nothing should have gone out")
    }

    @Test
    fun `a hero waiting on a plant response does not fire a redundant GET_BOMB_TARGET`() {
        bed.addHero(1)
        targets.requestTarget(1, cell(0, 0))
        targets.flush()

        targets.notifyPlanted(1)
        targets.requestTarget(1, cell(0, 0))

        assertFalse(targets.flush(), "planting must not double-request")
    }

    @Test
    fun `a response that never arrives stops blocking the hero after the in-flight expiry`() {
        bed.addHero(1)
        targets.notifyPlanted(1) // plant reply lost

        targets.requestTarget(1, cell(0, 0))
        assertFalse(targets.flush())

        clock.advance(ClientBombTargetManager.InFlightExpiryMs + 1)
        targets.requestTarget(1, cell(0, 0))
        assertTrue(targets.flush(), "the hero must recover on its own")
        assertNotNull(targets.tryGetTarget(1))
    }

    @Test
    fun `a hero the server omitted backs off instead of asking every frame`() {
        boot(TestMaps.empty())
        bed.addHero(1)

        targets.requestTarget(1, cell(0, 0))
        assertTrue(targets.flush())
        assertNull(targets.tryGetTarget(1))

        repeat(100) { targets.requestTarget(1, cell(0, 0)) }
        assertFalse(targets.flush(), "a per-frame caller must not produce a request per frame")

        clock.advance(ClientBombTargetManager.NoTargetRetryCooldownMs + 1)
        targets.requestTarget(1, cell(0, 0))
        assertTrue(targets.flush())
        assertEquals(2, targets.sentBatches.size)
    }

    @Test
    fun `a reject travels with the next request and gets a different cell back`() {
        bed.addHero(1)
        targets.requestTarget(1, cell(0, 0))
        targets.flush()
        val first = targets.tryGetTarget(1)
        assertNotNull(first)

        targets.rejectTarget(1, first)
        assertNull(targets.tryGetTarget(1), "rejecting drops the local copy too")

        clock.advance(ClientBombTargetManager.NoTargetRetryCooldownMs + 1)
        targets.requestTarget(1, cell(0, 0))
        targets.flush()

        val second = targets.tryGetTarget(1)
        assertNotNull(second)
        assertTrue(second != first, "the server was told this cell is unusable")
    }

    @Test
    fun `repeated rejects back off exponentially up to the 3s cap`() {
        bed.addHero(1)
        targets.requestTarget(1, cell(0, 0))
        targets.flush()

        // 0.25s, 0.5s, 1s, 2s, then capped at 3s.
        val expected = listOf(250L, 500L, 1_000L, 2_000L, 3_000L, 3_000L)
        for ((index, delay) in expected.withIndex()) {
            targets.rejectTarget(1, cell(index, 0))

            targets.requestTarget(1, cell(0, 0))
            assertFalse(targets.flush(), "reject #${index + 1} should still be backing off")

            clock.advance(delay + 1)
            targets.requestTarget(1, cell(0, 0))
            assertTrue(targets.flush(), "reject #${index + 1} should be allowed to retry after ${delay}ms")
        }
    }

    @Test
    fun `a successful plant resets the reject backoff`() {
        bed.addHero(1)
        targets.requestTarget(1, cell(0, 0))
        targets.flush()
        repeat(4) { targets.rejectTarget(1, cell(it, 0)) }

        targets.notifyPlanted(1)
        targets.onBombTarget(1, cell(5, 0))
        targets.rejectTarget(1, cell(5, 0))

        clock.advance(251)
        targets.requestTarget(1, cell(0, 0))
        assertTrue(targets.flush(), "the streak should have restarted at 0.25s, not stayed at 3s")
    }

    @Test
    fun `a mismatch drops the target so the hero re-bootstraps`() {
        bed.addHero(1)
        targets.requestTarget(1, cell(0, 0))
        targets.flush()
        assertNotNull(targets.tryGetTarget(1))

        targets.onBombTargetMismatch(1)

        assertNull(targets.tryGetTarget(1))
        targets.requestTarget(1, cell(0, 0))
        assertTrue(targets.flush())
    }

    @Test
    fun `a new map throws away every coordinate from the old one`() {
        for (id in 1..3) bed.addHero(id)
        for (id in 1..3) targets.requestTarget(id, cell(0, 0))
        targets.flush()

        targets.onNewMap()

        for (id in 1..3) assertNull(targets.tryGetTarget(id))
        for (id in 1..3) targets.requestTarget(id, cell(0, 0))
        assertTrue(targets.flush(), "no leftover backoff may block the re-bootstrap")
    }

    @Test
    fun `only the rejects of the heroes actually in the batch travel with it`() {
        bed.addHero(1)
        bed.addHero(2)
        targets.requestTarget(1, cell(0, 0))
        targets.requestTarget(2, cell(0, 0))
        targets.flush()

        val forHero2 = targets.tryGetTarget(2)
        assertNotNull(forHero2)
        targets.rejectTarget(2, forHero2)

        // Hero 2's reject must not ride along with hero 1's batch, and must not be lost.
        targets.onBombTargetMismatch(1)
        targets.requestTarget(1, cell(0, 0))
        targets.flush()

        clock.advance(ClientBombTargetManager.NoTargetRetryCooldownMs + 1)
        targets.requestTarget(2, cell(0, 0))
        targets.flush()
        val newForHero2 = targets.tryGetTarget(2)
        assertNotNull(newForHero2)
        assertTrue(newForHero2 != forHero2, "hero 2's reject was dropped instead of being delivered")
    }
}
