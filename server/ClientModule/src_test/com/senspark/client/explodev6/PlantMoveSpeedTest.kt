package com.senspark.client.explodev6

import com.senspark.game.controller.MapData
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.GameConstants
import io.mockk.every
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Tests UserBlockMapManagerImpl.isPlantTooFast. Server uses the wall clock, so tests control
// the budget (config, distance, speed) instead of time.
class PlantMoveSpeedTest {

    // Bricks along row 1, so any cell on row 0 is a legal target.
    private fun corridorMap(): MapData =
        TestMaps.withBricks(*(0 until 34 step 2).map { cell(it, 1) }.toTypedArray())

    private fun bed(
        speed: Int,
        toleranceMs: Int = 0,
        multiplier: Float = 1f,
        minTilesPerSecond: Float = 1f,
        reject: Boolean = true,
        enabled: Boolean = true,
    ): Pair<ServerTestBed, FakeGameClient> {
        val bed = ServerTestBed(corridorMap())
        every { bed.gameConfig.isCheckPlantMoveSpeed } returns enabled
        every { bed.gameConfig.isRejectPlantTooFast } returns reject
        every { bed.gameConfig.plantMoveSpeedToleranceMs } returns toleranceMs
        every { bed.gameConfig.plantMoveSpeedMultiplier } returns multiplier
        every { bed.gameConfig.plantMoveSpeedMin } returns minTilesPerSecond
        // bombCount = 2: tests plant twice without detonating, avoiding NO_BOMB_TO_PLANT.
        bed.addHero(1, speed = speed, bombCount = 2)
        return bed to FakeGameClient(bed)
    }

    // Plants once to set the anchor, then forces target to [next] and plants there immediately.
    private fun plantThenImmediatelyPlantAt(
        bed: ServerTestBed,
        client: FakeGameClient,
        anchor: Cell,
        next: Cell,
    ): FakeGameClient.PlantResult {
        client.getBombTarget(1, seed = anchor)
        val first = bed.blockMap.debugTarget(1)
        assertNotNull(first)
        assertTrue(client.startPlantBomb(1, 0, first).isOk, "the anchoring plant must be accepted")

        forceTarget(bed, next)
        return client.startPlantBomb(1, 1, next)
    }

    private fun forceTarget(bed: ServerTestBed, target: Cell) {
        val field = bed.blockMap.javaClass.getDeclaredField("_heroTargets")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (field.get(bed.blockMap) as MutableMap<Int, Cell>)[1] = target
    }

    private fun anchorCell(bed: ServerTestBed): Cell {
        val field = bed.blockMap.javaClass.getDeclaredField("_moveAnchors")
        field.isAccessible = true
        val anchor = (field.get(bed.blockMap) as Map<*, *>)[1] ?: error("no anchor")
        val cellField = anchor.javaClass.getDeclaredField("cell")
        cellField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return cellField.get(anchor) as Cell
    }

    @Test
    fun `a plant far from the last one, arriving instantly, is refused`() {
        val (bed, client) = bed(speed = 1, toleranceMs = 0)
        // 1 tile/second, 30 tiles away, zero tolerance: this needs 30 seconds of walking.
        val result = plantThenImmediatelyPlantAt(bed, client, anchor = cell(0, 0), next = cell(30, 0))
        assertEquals(ErrorCode.PLANT_TOO_FAST, result.errorCode)
    }

    @Test
    fun `it is logged as HACK_SPEED`() {
        val (bed, client) = bed(speed = 1, toleranceMs = 0)
        plantThenImmediatelyPlantAt(bed, client, anchor = cell(0, 0), next = cell(30, 0))
        assertTrue(
            bed.hackLogs.any { it.first == GameConstants.LOG_HACK_TYPE.HACK_SPEED },
            "a rejected plant must reach the hack log, not just the error path",
        )
    }

    @Test
    fun `a refused plant records nothing -- no bomb, no new anchor, target untouched`() {
        val (bed, client) = bed(speed = 1, toleranceMs = 0)
        val result = plantThenImmediatelyPlantAt(bed, client, anchor = cell(0, 0), next = cell(30, 0))
        assertEquals(ErrorCode.PLANT_TOO_FAST, result.errorCode)

        assertNull(bed.blockMap.takePlantedBomb(1, 1, 30, 0), "no bomb may be recorded")
        assertEquals(cell(30, 0), bed.blockMap.debugTarget(1), "the hero keeps the target it held")
        assertTrue(anchorCell(bed) != cell(30, 0), "the anchor must not move to a refused plant")
    }

    @Test
    fun `the same plant is accepted once the tolerance covers it`() {
        val (bed, client) = bed(speed = 1, toleranceMs = 60_000)
        val result = plantThenImmediatelyPlantAt(bed, client, anchor = cell(0, 0), next = cell(30, 0))
        assertTrue(result.isOk, "error ${result.errorCode}")
    }

    @Test
    fun `a fast hero covers the same distance inside the same budget`() {
        // 30 tiles at 300 tiles/s needs 100ms, covered by the tolerance.
        val (bed, client) = bed(speed = 300, toleranceMs = 200)
        assertTrue(plantThenImmediatelyPlantAt(bed, client, cell(0, 0), cell(30, 0)).isOk)
    }

    @Test
    fun `the speed multiplier is the headroom it claims to be`() {
        // 4 tiles at speed 1: 4000ms at 1x (too fast), 1000ms at 4x (fits 1100ms tolerance).
        val (tight, tightClient) = bed(speed = 1, toleranceMs = 1_100, multiplier = 1f)
        assertEquals(
            ErrorCode.PLANT_TOO_FAST,
            plantThenImmediatelyPlantAt(tight, tightClient, cell(0, 0), cell(4, 0)).errorCode,
        )

        val (loose, looseClient) = bed(speed = 1, toleranceMs = 1_100, multiplier = 4f)
        assertTrue(plantThenImmediatelyPlantAt(loose, looseClient, cell(0, 0), cell(4, 0)).isOk)
    }

    @Test
    fun `a speed-zero hero does not divide by zero`() {
        val (bed, client) = bed(speed = 0, toleranceMs = 5_000, minTilesPerSecond = 1f)
        val result = plantThenImmediatelyPlantAt(bed, client, cell(0, 0), cell(4, 0))
        // 4 tiles at the 1 tile/s floor needs 4000ms, inside the 5000ms tolerance.
        assertTrue(result.isOk, "error ${result.errorCode}")
    }

    @Test
    fun `detect-only mode logs the hack but lets the plant through`() {
        val (bed, client) = bed(speed = 1, toleranceMs = 0, reject = false)
        val result = plantThenImmediatelyPlantAt(bed, client, cell(0, 0), cell(30, 0))

        assertTrue(result.isOk, "detect-only mode must not reject")
        assertTrue(bed.hackLogs.any { it.first == GameConstants.LOG_HACK_TYPE.HACK_SPEED })
        assertNotNull(bed.blockMap.takePlantedBomb(1, 1, 30, 0), "the bomb is still recorded")
    }

    @Test
    fun `the check can be turned off entirely`() {
        val (bed, client) = bed(speed = 1, toleranceMs = 0, enabled = false)
        assertTrue(plantThenImmediatelyPlantAt(bed, client, cell(0, 0), cell(30, 0)).isOk)
        assertTrue(bed.hackLogs.isEmpty(), "nothing should be logged when the check is off")
    }

    @Test
    fun `with no anchor at all the check cannot fire`() {
        val (bed, client) = bed(speed = 1, toleranceMs = 0)
        forceTarget(bed, cell(30, 0))
        assertTrue(client.startPlantBomb(1, 0, cell(30, 0)).isOk)
    }

    @Test
    fun `the map's first plant is anchored on the client's own claim`() {
        // Known limit: first anchor is the untrusted seed, so the first plant is always free.
        for (claimedSeed in listOf(cell(0, 0), cell(30, 0))) {
            val bed = ServerTestBed(corridorMap())
            every { bed.gameConfig.plantMoveSpeedToleranceMs } returns 0
            every { bed.gameConfig.plantMoveSpeedMultiplier } returns 1f
            bed.addHero(1, speed = 1)
            val client = FakeGameClient(bed)

            val target = client.getBombTarget(1, seed = claimedSeed)
            assertEquals(claimedSeed, target, "the pick follows the claim")
            assertTrue(client.startPlantBomb(1, 0, claimedSeed).isOk)
        }
    }

    @Test
    fun `replanting on the anchor cell itself is never too fast`() {
        val (bed, client) = bed(speed = 1, toleranceMs = 0)
        client.getBombTarget(1, seed = cell(0, 0))
        val first = bed.blockMap.debugTarget(1)
        assertNotNull(first)
        assertTrue(client.startPlantBomb(1, 0, first).isOk)

        forceTarget(bed, first)
        assertTrue(client.startPlantBomb(1, 1, first).isOk)
    }

    @Test
    fun `a wrong-cell plant reports a mismatch, not a speed hack`() {
        // Cell checks run first so a desync isn't logged as cheating.
        val (bed, client) = bed(speed = 1, toleranceMs = 0)
        client.getBombTarget(1, seed = cell(0, 0))
        val held = bed.blockMap.debugTarget(1)
        assertNotNull(held)
        assertTrue(client.startPlantBomb(1, 0, held).isOk)

        forceTarget(bed, cell(30, 0))
        val result = client.startPlantBomb(1, 1, cell(28, 0))

        assertEquals(ErrorCode.PLANT_TARGET_MISMATCH, result.errorCode)
        assertTrue(bed.hackLogs.isEmpty(), "a desync must not be logged as a speed hack")
    }

    @Test
    fun `an honest walker planting at real speed is accepted`() {
        // Only wall-clock test: the hero really waits out the travel time.
        val bed = ServerTestBed(corridorMap())
        every { bed.gameConfig.plantMoveSpeedToleranceMs } returns 0
        every { bed.gameConfig.plantMoveSpeedMultiplier } returns 1.15f
        bed.addHero(1, speed = 20, bombCount = 2) // 20 tiles/s -> 50ms per tile
        val client = FakeGameClient(bed)

        client.getBombTarget(1, seed = cell(0, 0))
        val first = bed.blockMap.debugTarget(1)
        assertNotNull(first)
        assertTrue(client.startPlantBomb(1, 0, first).isOk)

        val next = cell(first.i + 8, first.j)
        forceTarget(bed, next)
        Thread.sleep(8 * 1000L / 20)

        assertTrue(client.startPlantBomb(1, 1, next).isOk, "an honest walk must never be refused")
    }
}
