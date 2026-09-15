package com.senspark.client.explodev6

import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.GameConstants
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// GET_BOMB_TARGET, see server/docs/explode_v6.md.
class GetBombTargetTest {

    private lateinit var bed: ServerTestBed
    private lateinit var client: FakeGameClient

    private fun boot(map: com.senspark.game.controller.MapData) {
        bed = ServerTestBed(map)
        client = FakeGameClient(bed)
    }

    @BeforeEach
    fun setUp() {
        boot(TestMaps.withBricks(cell(6, 0)))
    }

    @Test
    fun `hands a working hero a legal, brick-adjacent cell`() {
        bed.addHero(1)
        val target = client.getBombTarget(1, seed = cell(0, 0))
        assertNotNull(target)
        assertTrue(bed.map.canSetBoom(target.i, target.j), "$target must be plantable")
        assertTrue(bed.map.hasBlockAround(target.i, target.j), "$target must have something to bomb")
    }

    @Test
    fun `echoes hero_type so the client can rebuild a full HeroId`() {
        bed.addHero(1, type = EnumConstants.HeroType.TON)
        val entry = client.getBombTargets(mapOf(1 to cell(0, 0))).single()
        assertEquals(1, entry.heroId)
        assertEquals(EnumConstants.HeroType.TON.value, entry.heroType)
    }

    @Test
    fun `answers a whole batch in one request`() {
        for (id in 1..5) bed.addHero(id)
        val seeds = (1..5).associateWith { cell(0, 0) }
        val results = client.getBombTargets(seeds)
        assertEquals(5, results.size)
        assertEquals((1..5).toSet(), results.map { it.heroId }.toSet())
    }

    @Test
    fun `gives heroes in the same batch distinct cells`() {
        for (id in 1..3) bed.addHero(id)
        val results = client.getBombTargets((1..3).associateWith { cell(0, 0) })
        val cells = results.map { it.location }
        assertEquals(cells.size, cells.toSet().size, "two heroes were sent to the same cell: $cells")
    }

    @Test
    fun `omits a hero that is not active`() {
        bed.addHero(1, active = false)
        bed.addHero(2)
        val results = client.getBombTargets(mapOf(1 to cell(0, 0), 2 to cell(0, 0)))
        assertEquals(listOf(2), results.map { it.heroId })
    }

    @Test
    fun `omits a hero that is not working`() {
        bed.addHero(1, stage = GameConstants.BOMBER_STAGE.SLEEP)
        bed.addHero(2)
        val results = client.getBombTargets(mapOf(1 to cell(0, 0), 2 to cell(0, 0)))
        assertEquals(listOf(2), results.map { it.heroId })
    }

    @Test
    fun `omits an unknown hero id without failing the whole batch`() {
        bed.addHero(2)
        val results = client.getBombTargets(mapOf(999 to cell(0, 0), 2 to cell(0, 0)))
        assertEquals(listOf(2), results.map { it.heroId })
    }

    @Test
    fun `omits every hero when no cell on the map can be bombed`() {
        boot(TestMaps.empty())
        bed.addHero(1)
        // Still a success response; omission means no target.
        assertTrue(client.getBombTargets(mapOf(1 to cell(0, 0))).isEmpty())
    }

    @Test
    fun `the same hero asking twice keeps its outstanding target`() {
        bed.addHero(1)
        val first = client.getBombTarget(1, seed = cell(0, 0))
        val second = client.getBombTarget(1, seed = cell(30, 16))
        assertEquals(first, second, "a hero holds exactly one target until it plants or rejects")
    }

    @Test
    fun `a rejected cell is never handed back to that hero again`() {
        bed.addHero(1)
        val first = client.getBombTarget(1, seed = cell(0, 0))
        assertNotNull(first)

        val second = client.getBombTarget(1, seed = cell(0, 0), reject = first)
        assertNotNull(second)
        assertNotEquals(first, second, "rejecting must move the hero on, not loop on the same cell")
    }

    @Test
    fun `a reject from one hero does not move another hero off that cell`() {
        bed.addHero(1)
        bed.addHero(2)
        val forHero2 = client.getBombTarget(2, seed = cell(0, 0))
        assertNotNull(forHero2)
        client.getBombTarget(1, seed = cell(0, 0), reject = forHero2)
        assertEquals(forHero2, client.getBombTarget(2, seed = cell(0, 0)))
    }

    @Test
    fun `the seed hint only steers a hero that has never planted here`() {
        boot(TestMaps.withBricks(cell(2, 0), cell(30, 16)))
        bed.addHero(1)
        bed.disableMoveSpeedCheck()

        val nearStart = client.getBombTarget(1, seed = cell(0, 0))
        assertNotNull(nearStart)
        assertTrue(manhattan(nearStart, cell(0, 0)) < manhattan(nearStart, cell(30, 16)))

        // After a plant, the last-planted cell is the seed and the client hint is ignored.
        client.startPlantBomb(1, 0, nearStart)
        val next = client.getBombTarget(1, seed = cell(30, 16))
        assertNotNull(next)
        assertTrue(
            manhattan(next, nearStart) < manhattan(next, cell(30, 16)),
            "after a plant the server seeds from the plant cell, not from the client's claim",
        )
    }

    @Test
    fun `a cell holding a live bomb is never handed out`() {
        boot(TestMaps.withBricks(cell(2, 0)))
        bed.addHero(1)
        bed.addHero(2)
        bed.disableMoveSpeedCheck()

        val forHero1 = client.getBombTarget(1, seed = cell(0, 0))
        assertNotNull(forHero1)
        assertTrue(client.startPlantBomb(1, 0, forHero1).isOk)

        val forHero2 = client.getBombTarget(2, seed = cell(0, 0))
        assertNotEquals(forHero1, forHero2, "hero 2 was sent onto hero 1's live bomb")
    }

    @Test
    fun `a block-pass hero standing on a brick still gets a target`() {
        // BFS finds nothing from a brick seed; findAnyBombTarget covers it.
        boot(
            TestMaps.fromAscii(
                "BBB",
                "B#B",
                "BBB",
            )
        )
        bed.addHero(1, abilities = setOf(GameConstants.BOMBER_ABILITY.BLOCK_PASS))
        val target = client.getBombTarget(1, seed = cell(1, 0))
        assertNotNull(target, "a hero standing on a brick must still be given somewhere to go")
        assertTrue(bed.map.canSetBoom(target.i, target.j))
        assertTrue(bed.map.hasBlockAround(target.i, target.j))
    }

    @Test
    fun `a rejected fallback target is not handed straight back`() {
        // Fallback is deterministic, so it must honor rejects or it returns the same cell forever.
        boot(
            TestMaps.fromAscii(
                "BB",
                "B#",
            )
        )
        bed.addHero(1)

        // Only two brick-adjacent cells: (0,2) and (2,0).
        val first = client.getBombTarget(1, seed = cell(0, 0))
        assertEquals(cell(0, 2), first)

        val second = client.getBombTarget(1, seed = cell(0, 0), reject = first)
        assertEquals(cell(2, 0), second, "rejecting an unreachable target must move the hero on")
    }

    @Test
    fun `the fallback starts over once every cell has been rejected`() {
        boot(
            TestMaps.fromAscii(
                "BB",
                "B#",
            )
        )
        bed.addHero(1)

        val first = client.getBombTarget(1, seed = cell(0, 0))
        val second = client.getBombTarget(1, seed = cell(0, 0), reject = first)
        val third = client.getBombTarget(1, seed = cell(0, 0), reject = second)

        assertEquals(cell(0, 2), first)
        assertEquals(cell(2, 0), second)
        assertEquals(first, third, "with both candidates rejected the rotation restarts")
    }

    @Test
    fun `a stale target is replaced rather than handed back`() {
        bed.addHero(1)
        val first = client.getBombTarget(1, seed = cell(0, 0))
        assertNotNull(first)

        bed.map.removeBlockMap(bed.map.getBlockMap(6, 0))
        bed.map.addBlockMap(TestMaps.brick(20, 10))

        val second = client.getBombTarget(1, seed = cell(0, 0))
        assertNotNull(second)
        assertNotEquals(first, second)
        assertTrue(bed.map.hasBlockAround(second.i, second.j))
    }

    @Test
    fun `a hero belonging to another chain is not targeted`() {
        val hero = bed.addHero(1)
        io.mockk.every { hero.details.dataType } returns EnumConstants.DataType.TON
        assertTrue(client.getBombTargets(mapOf(1 to cell(0, 0))).isEmpty())
    }

    @Test
    fun `no target survives the map regenerating`() {
        boot(TestMaps.withBricks(cell(2, 0)))
        bed.addHero(1)
        bed.disableMoveSpeedCheck()

        val target = client.getBombTarget(1, seed = cell(0, 0))
        assertNotNull(target)
        assertTrue(client.startPlantBomb(1, 0, target).isOk)
        assertTrue(client.fireFuse(1, 0, target).pushed)
        assertTrue(bed.pushes.any { it.command == com.senspark.game.declare.SFSCommand.PVE_NEW_MAP })

        assertNull(bed.blockMap.debugTarget(1))
    }
}
