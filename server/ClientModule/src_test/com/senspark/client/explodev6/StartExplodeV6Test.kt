package com.senspark.client.explodev6

import com.senspark.game.controller.MapData
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.GameConstants
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Server fuse (detonateBomb): when a push is sent, and that the plant record is always released.
// fireFuse stands in for waiting time_bomb_explode ms.
class StartExplodeV6Test {

    private lateinit var bed: ServerTestBed
    private lateinit var client: FakeGameClient

    private fun boot(map: MapData = TestMaps.withBricks(cell(6, 0), cell(10, 4), cell(20, 8))) {
        bed = ServerTestBed(map)
        bed.disableMoveSpeedCheck()
        client = FakeGameClient(bed)
    }

    @BeforeEach
    fun setUp() = boot()

    private fun plantOnce(heroId: Int, bombNo: Int = 0, seed: Cell = cell(0, 0)): Cell {
        val target = client.getBombTarget(heroId, seed) ?: error("no target")
        assertTrue(client.startPlantBomb(heroId, bombNo, target).isOk)
        return target
    }

    @Test
    fun `a bomb detonates where it was planted`() {
        bed.addHero(1)
        val at = plantOnce(1)

        val result = client.fireFuse(1, 0, at)

        assertTrue(result.pushed, "the fuse fired but produced no push")
        assertEquals(0, result.bombNo)
        assertEquals(at, result.cell)
    }

    @Test
    fun `the blast removes the bricks it destroyed from the server map`() {
        boot(TestMaps.withBricks(cell(2, 0), cell(20, 8)))
        bed.addHero(1)
        val at = plantOnce(1)
        assertEquals(cell(1, 0), at)

        val result = client.fireFuse(1, 0, at)

        assertNull(bed.map.getBlockMap(2, 0), "the brick the blast hit must be gone server-side")
        assertTrue(result.blocks.any { it.first == 2 && it.second == 0 && it.third <= 0 })
    }

    @Test
    fun `a bomb that was never planted has no fuse to fire`() {
        bed.addHero(1)
        assertFalse(bed.fireFuse(1, 0, 5, 0), "nothing was ever armed for this bomb")
    }

    @Test
    fun `the same plant cannot be taken twice`() {
        bed.addHero(1)
        val at = plantOnce(1)
        assertTrue(client.fireFuse(1, 0, at).pushed)

        assertNull(bed.blockMap.takePlantedBomb(1, 0, at.i, at.j), "nothing should be left to take")
    }

    @Test
    fun `a multi-bomb hero's bombs detonate independently even sharing a slot id later`() {
        bed.addHero(1, bombCount = 2)
        val first = plantOnce(1, bombNo = 0)
        val second = bed.blockMap.debugTarget(1)
        assertNotNull(second)
        assertTrue(client.startPlantBomb(1, 1, second).isOk)

        assertTrue(client.fireFuse(1, 0, first).pushed, "the first bomb must detonate")
        assertTrue(client.fireFuse(1, 1, second).pushed, "and so must the second")
    }

    @Test
    fun `an out-of-energy hero's fuse still releases its bomb's cell`() {
        // takePlantedBomb runs before hero guards, so the cell is never stuck excluded.
        val hero = bed.addHero(1)
        val at = plantOnce(1)
        every { hero.energy } returns 0

        val result = client.fireFuse(1, 0, at)

        assertTrue(result.pushed, "an exhausted hero still gets a push -- with energy 0 and no rewards")
        assertEquals(0, result.energy)
        assertNull(bed.blockMap.takePlantedBomb(1, 0, at.i, at.j), "the plant record must have been released")
    }

    @Test
    fun `a hero that went inactive has its fuse fire with no push, cell still released`() {
        val hero = bed.addHero(1)
        val at = plantOnce(1)
        every { hero.isActive } returns false

        val result = client.fireFuse(1, 0, at)

        assertFalse(result.pushed, "an inactive hero gets no RESPONSE_EXPLODE -- there is no request to answer")
        assertNull(bed.blockMap.takePlantedBomb(1, 0, at.i, at.j), "the cell would stay excluded forever otherwise")
    }

    @Test
    fun `a hero that fell asleep has its fuse fire with no push, cell still released`() {
        val hero = bed.addHero(1)
        val at = plantOnce(1)
        every { hero.stage } returns GameConstants.BOMBER_STAGE.SLEEP

        val result = client.fireFuse(1, 0, at)

        assertFalse(result.pushed)
        assertNull(bed.blockMap.takePlantedBomb(1, 0, at.i, at.j))
    }

    @Test
    fun `a hero removed from heroFiManager after planting still releases its bomb's cell`() {
        bed.addHero(1)
        val at = plantOnce(1)
        bed.removeHero(1)

        val result = client.fireFuse(1, 0, at)

        assertFalse(result.pushed)
        assertNull(bed.blockMap.takePlantedBomb(1, 0, at.i, at.j))
    }

    @Test
    fun `emptying the map regenerates it and clears every hero's V6 state`() {
        boot(TestMaps.withBricks(cell(2, 0)))
        bed.addHero(1)
        val at = plantOnce(1)

        assertTrue(client.fireFuse(1, 0, at).pushed)

        assertTrue(bed.pushes.any { it.command == com.senspark.game.declare.SFSCommand.PVE_NEW_MAP })
        assertNull(bed.blockMap.debugTarget(1), "coordinates are only valid against their own map")
    }
}
