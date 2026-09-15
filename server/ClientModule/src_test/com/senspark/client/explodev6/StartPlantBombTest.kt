package com.senspark.client.explodev6

import com.senspark.game.controller.MapData
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.GameConstants
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// START_PLANT_BOMB, see server/docs/explode_v6.md.
class StartPlantBombTest {

    private lateinit var bed: ServerTestBed
    private lateinit var client: FakeGameClient

    private fun boot(map: MapData = TestMaps.withBricks(cell(6, 0), cell(10, 4), cell(20, 8))) {
        bed = ServerTestBed(map)
        bed.disableMoveSpeedCheck()
        client = FakeGameClient(bed)
    }

    @BeforeEach
    fun setUp() = boot()

    private fun targetFor(heroId: Int, seed: Cell = cell(0, 0)): Cell =
        client.getBombTarget(heroId, seed) ?: error("no target for hero $heroId")

    @Test
    fun `a plant on the assigned cell is accepted and answers with the next target`() {
        bed.addHero(1)
        val target = targetFor(1)

        val result = client.startPlantBomb(1, 0, target)

        assertTrue(result.isOk, "error ${result.errorCode}")
        val next = result.requireNextTarget()
        assertNotEquals(target, next, "the next target must not be the cell the bomb is sitting on")
        assertTrue(bed.map.hasBlockAround(next.i, next.j))
    }

    @Test
    fun `the next target is what the client then holds -- no extra GET_BOMB_TARGET`() {
        bed.addHero(1)
        val target = targetFor(1)
        val next = client.startPlantBomb(1, 0, target).requireNextTarget()
        assertEquals(next, bed.blockMap.debugTarget(1))
    }

    @Test
    fun `planting somewhere other than the assigned cell is refused`() {
        bed.addHero(1)
        val target = targetFor(1)
        val elsewhere = cell(target.i + 2, target.j)

        val result = client.startPlantBomb(1, 0, elsewhere)

        assertEquals(ErrorCode.PLANT_TARGET_MISMATCH, result.errorCode)
    }

    @Test
    fun `planting while holding no target at all is refused`() {
        bed.addHero(1)
        val result = client.startPlantBomb(1, 0, cell(5, 0))
        assertEquals(ErrorCode.PLANT_TARGET_MISMATCH, result.errorCode)
    }

    @Test
    fun `the same target cannot be planted on twice`() {
        bed.addHero(1)
        val target = targetFor(1)
        assertTrue(client.startPlantBomb(1, 0, target).isOk)

        val again = client.startPlantBomb(1, 1, target)
        assertEquals(ErrorCode.PLANT_TARGET_MISMATCH, again.errorCode)
    }

    @Test
    fun `a refused plant records nothing and leaves the held target alone`() {
        bed.addHero(1)
        val target = targetFor(1)

        assertEquals(ErrorCode.PLANT_TARGET_MISMATCH, client.startPlantBomb(1, 0, cell(0, 16)).errorCode)

        assertEquals(target, bed.blockMap.debugTarget(1), "the hero must keep the target it was holding")
        assertNull(bed.blockMap.takePlantedBomb(1, 0, cell(0, 16).i, cell(0, 16).j), "no bomb may be recorded")
        assertNull(bed.blockMap.takePlantedBomb(1, 0, target.i, target.j), "no bomb may be recorded")
    }

    @Test
    fun `a cell that stopped being plantable is refused even when it is the held target`() {
        bed.addHero(1)
        val target = targetFor(1)
        bed.map.addBlockMap(TestMaps.brick(target.i, target.j))

        assertEquals(ErrorCode.PLANT_TARGET_MISMATCH, client.startPlantBomb(1, 0, target).errorCode)
    }

    @Test
    fun `an inactive hero cannot plant`() {
        val hero = bed.addHero(1)
        val target = targetFor(1)
        io.mockk.every { hero.isActive } returns false

        assertEquals(ErrorCode.BOMBERMAN_ACTIVE_INVALID, client.startPlantBomb(1, 0, target).errorCode)
    }

    @Test
    fun `a sleeping hero cannot plant`() {
        val hero = bed.addHero(1)
        val target = targetFor(1)
        io.mockk.every { hero.stage } returns GameConstants.BOMBER_STAGE.SLEEP

        assertEquals(ErrorCode.BOMBERMAN_IS_NOT_WORKING, client.startPlantBomb(1, 0, target).errorCode)
    }

    @Test
    fun `an unknown hero id is refused`() {
        assertEquals(ErrorCode.BOMBERMAN_NULL, client.startPlantBomb(404, 0, cell(5, 0)).errorCode)
    }

    @Test
    fun `NO_BOMB_TARGET when the plant succeeded but nothing is left to bomb`() {
        boot(TestMaps.withBricks(cell(2, 0)))
        bed.addHero(1)
        val target = targetFor(1)
        // Remove the only brick so the follow-up pick finds nothing.
        bed.map.removeBlockMap(bed.map.getBlockMap(2, 0))

        val result = client.startPlantBomb(1, 0, target)

        assertEquals(ErrorCode.NO_BOMB_TARGET, result.errorCode)
        assertNotNull(
            bed.blockMap.takePlantedBomb(1, 0, target.i, target.j),
            "the bomb was accepted even though no next target could be found",
        )
    }

    @Test
    fun `planting clears the rejects this hero had accumulated`() {
        // Reachable brick-adjacent cells: (1,0) and (2,1).
        boot(TestMaps.withBricks(cell(2, 0)))
        bed.addHero(1)

        assertEquals(cell(1, 0), targetFor(1))
        assertEquals(cell(2, 1), client.getBombTarget(1, cell(0, 0), reject = cell(1, 0)))

        assertTrue(client.startPlantBomb(1, 0, cell(2, 1)).isOk)

        // (1,0) is a candidate again; had the reject survived the plant, this would be null.
        assertEquals(cell(1, 0), bed.blockMap.debugTarget(1))
    }

    @Test
    fun `two heroes each get their own target and neither steals the other's`() {
        bed.addHero(1)
        bed.addHero(2)
        val t1 = targetFor(1)
        val t2 = targetFor(2)
        assertNotEquals(t1, t2)

        assertEquals(ErrorCode.PLANT_TARGET_MISMATCH, client.startPlantBomb(2, 0, t1).errorCode)
        assertTrue(client.startPlantBomb(1, 0, t1).isOk)
        assertTrue(client.startPlantBomb(2, 0, t2).isOk)
    }

    @Test
    fun `a 1-bomb hero cannot plant again while its live bomb is still unexploded`() {
        bed.addHero(1, bombCount = 1)
        val first = targetFor(1)
        assertTrue(client.startPlantBomb(1, 0, first).isOk)

        val next = bed.blockMap.debugTarget(1) ?: error("no next target")
        val result = client.startPlantBomb(1, 0, next)

        assertEquals(ErrorCode.NO_BOMB_TO_PLANT, result.errorCode)
        assertNull(bed.blockMap.takePlantedBomb(1, 0, next.i, next.j), "no bomb may be recorded")
        assertEquals(next, bed.blockMap.debugTarget(1), "the hero keeps the target it held")
    }

    @Test
    fun `a 2-bomb hero can hold two live bombs but not a third`() {
        bed.addHero(1, bombCount = 2)
        val first = targetFor(1)
        assertTrue(client.startPlantBomb(1, 0, first).isOk)

        val second = bed.blockMap.debugTarget(1) ?: error("no next target")
        val secondResult = client.startPlantBomb(1, 1, second)
        assertTrue(secondResult.isOk, "error ${secondResult.errorCode}")

        val third = bed.blockMap.debugTarget(1) ?: error("no next target")
        assertEquals(ErrorCode.NO_BOMB_TO_PLANT, client.startPlantBomb(1, 0, third).errorCode)
    }

    @Test
    fun `capacity frees up again once the live bomb's fuse fires`() {
        bed.addHero(1, bombCount = 1)
        val first = targetFor(1)
        assertTrue(client.startPlantBomb(1, 0, first).isOk)

        val next = bed.blockMap.debugTarget(1) ?: error("no next target")
        assertEquals(ErrorCode.NO_BOMB_TO_PLANT, client.startPlantBomb(1, 0, next).errorCode)

        assertTrue(bed.fireFuse(1, 0, first.i, first.j), "the fuse should still be pending")

        val result = client.startPlantBomb(1, 0, next)
        assertTrue(result.isOk, "error ${result.errorCode}")
    }

    @Test
    fun `each plant seeds the next target from where the bomb was just planted`() {
        // High bombCount: bombs are never detonated here, so capacity must not block.
        bed.addHero(1, bombCount = 10)
        var at = targetFor(1)
        repeat(5) {
            val next = client.startPlantBomb(1, it, at).requireNextTarget()
            assertTrue(
                manhattan(at, next) <= 8,
                "next target $next is ${manhattan(at, next)} tiles from the plant at $at -- " +
                    "the BFS is not seeding from the just-planted cell",
            )
            at = next
        }
    }
}
