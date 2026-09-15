package com.senspark.client.explodev6

import com.senspark.game.declare.GameConstants
import com.senspark.game.manager.blockMap.UserBlockMapManagerImpl
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Per-hero V6 server state lifecycle (targets, plants, rejects, anchors).
class ServerTargetStateTest {

    private lateinit var bed: ServerTestBed
    private lateinit var client: FakeGameClient

    private fun boot(map: com.senspark.game.controller.MapData) {
        bed = ServerTestBed(map)
        bed.disableMoveSpeedCheck()
        client = FakeGameClient(bed)
    }

    @BeforeEach
    fun setUp() {
        boot(TestMaps.withBricks(cell(6, 0), cell(10, 4), cell(20, 8)))
    }

    @Test
    fun `a live bomb blocks its cell until its own fuse fires`() {
        // Bricks survive the blast so the target stays brick-adjacent.
        boot(
            TestMaps.withBlocks(
                TestMaps.brick(2, 0, hp = 3),
                TestMaps.brick(20, 8, hp = 3),
            )
        )
        bed.addHero(1)
        bed.addHero(2)

        val target = client.getBombTarget(1, cell(0, 0))
        assertNotNull(target)
        assertTrue(client.startPlantBomb(1, 0, target).isOk)

        assertNotEquals(target, client.getBombTarget(2, cell(0, 0)))

        client.fireFuse(1, 0, target)

        client.getBombTarget(2, cell(0, 0), reject = bed.blockMap.debugTarget(2))
        assertEquals(target, bed.blockMap.debugTarget(2))
    }

    @Test
    fun `an exploded bomb frees its cell immediately`() {
        // Bricks survive the blast so the target stays brick-adjacent.
        boot(
            TestMaps.withBlocks(
                TestMaps.brick(0, 0, hp = 3),
                TestMaps.brick(2, 0, hp = 3),
                TestMaps.brick(20, 8, hp = 3),
            )
        )
        bed.addHero(1)
        bed.addHero(2)

        assertEquals(cell(1, 0), client.getBombTarget(1, cell(0, 0)))
        assertTrue(client.startPlantBomb(1, 0, cell(1, 0)).isOk)
        assertTrue(client.fireFuse(1, 0, cell(1, 0)).pushed)

        assertEquals(cell(1, 0), client.getBombTarget(2, cell(0, 0)))
    }

    @Test
    fun `two heroes may share a cell when nothing else is left`() {
        boot(TestMaps.withBricks(cell(2, 0)))
        bed.addHero(1)
        bed.addHero(2)
        bed.addHero(3)
        bed.addHero(4)

        val targets = (1..4).map { client.getBombTarget(it, cell(0, 0)) }
        assertTrue(targets.all { it != null }, "a hero was left with no target: $targets")
        assertTrue(targets.toSet().size < 4, "with only 2 usable cells, some heroes must share")
    }

    @Test
    fun `rejects are dropped rather than starving the hero`() {
        boot(TestMaps.withBricks(cell(2, 0)))
        bed.addHero(1)

        assertEquals(cell(1, 0), client.getBombTarget(1, cell(0, 0)))
        assertEquals(cell(2, 1), client.getBombTarget(1, cell(0, 0), reject = cell(1, 0)))

        val afterBothRejected = client.getBombTarget(1, cell(0, 0), reject = cell(2, 1))
        assertNotNull(afterBothRejected, "the hero was starved by its own rejects")
    }

    @Test
    fun `the anchor moves only on an accepted plant, never on a re-pick`() {
        boot(TestMaps.withBricks(cell(2, 0), cell(20, 8)))
        bed.addHero(1)

        client.getBombTarget(1, cell(0, 0))
        val anchorAfterBootstrap = anchorOf(bed.blockMap, 1)

        client.getBombTarget(1, cell(0, 0), reject = bed.blockMap.debugTarget(1))
        assertEquals(anchorAfterBootstrap, anchorOf(bed.blockMap, 1), "a re-pick must not re-anchor")

        val target = bed.blockMap.debugTarget(1)
        assertNotNull(target)
        assertTrue(client.startPlantBomb(1, 0, target).isOk)
        assertEquals(target, anchorOf(bed.blockMap, 1), "a plant is the strongest position fix we get")
    }

    @Test
    fun `the map regenerating clears every per-hero map`() {
        boot(TestMaps.withBricks(cell(2, 0)))
        bed.addHero(1)
        bed.addHero(2)

        val t1 = client.getBombTarget(1, cell(0, 0))
        assertNotNull(t1)
        client.getBombTarget(2, cell(0, 0))
        assertTrue(client.startPlantBomb(1, 0, t1).isOk)

        assertTrue(client.fireFuse(1, 0, t1).pushed)

        assertNull(bed.blockMap.debugTarget(1))
        assertNull(bed.blockMap.debugTarget(2))
        assertTrue(anchorsOf(bed.blockMap).isEmpty(), "anchors from the old map would fire false positives")
    }

    @Test
    fun `debugTarget reports exactly what the hero is allowed to plant on`() {
        bed.addHero(1)
        assertNull(bed.blockMap.debugTarget(1))

        val target = client.getBombTarget(1, cell(0, 0))
        assertEquals(target, bed.blockMap.debugTarget(1))

        assertNotNull(target)
        assertTrue(client.startPlantBomb(1, 0, target).isOk)
        assertNotEquals(target, bed.blockMap.debugTarget(1), "the target was consumed by the plant")
    }

    @Test
    fun `a hero with the block-pass ability is not treated differently by the server`() {
        boot(TestMaps.fromAscii("BBB", "B#B", "BBB"))
        bed.addHero(1, abilities = setOf(GameConstants.BOMBER_ABILITY.BLOCK_PASS))
        bed.addHero(2)

        val forBlockPass = client.getBombTarget(1, cell(1, 0))
        val forNormal = client.getBombTarget(2, cell(1, 0))
        assertNotNull(forBlockPass)
        assertNotNull(forNormal)
    }

    // ---- reflection helpers ----

    private fun anchorsOf(blockMap: UserBlockMapManagerImpl): Map<*, *> {
        val field = UserBlockMapManagerImpl::class.java.getDeclaredField("_moveAnchors")
        field.isAccessible = true
        return field.get(blockMap) as Map<*, *>
    }

    private fun anchorOf(blockMap: UserBlockMapManagerImpl, heroId: Int): Cell {
        val anchor = anchorsOf(blockMap)[heroId] ?: error("hero $heroId has no anchor")
        val cellField = anchor.javaClass.getDeclaredField("cell")
        cellField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return cellField.get(anchor) as Cell
    }
}
