package com.senspark.client.explodev6

import com.senspark.game.declare.GameConstants.MAP_MAX_COL
import com.senspark.game.declare.GameConstants.MAP_MAX_ROW
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// MapData.findBombTarget / findAnyBombTarget directly, to pin which cell is picked.
class BombTargetPickerTest {

    @Test
    fun `picks the nearest brick-adjacent cell by walking hops`() {
        val map = TestMaps.withBricks(cell(6, 0))
        val target = map.findBombTarget(0, 0, emptySet())
        assertEquals(cell(5, 0), target)
    }

    @Test
    fun `the seed cell is itself a candidate`() {
        // A walled-in hero can only bomb where it stands.
        val map = TestMaps.withBricks(cell(1, 0), cell(0, 1))
        assertEquals(cell(0, 0), map.findBombTarget(0, 0, emptySet()))
    }

    @Test
    fun `never returns a cell with no brick beside it`() {
        val map = TestMaps.empty()
        assertNull(map.findBombTarget(0, 0, emptySet()))
    }

    @Test
    fun `skips excluded cells and takes the next-nearest instead`() {
        val map = TestMaps.withBricks(cell(6, 0))
        // (6,1) and (7,0) are equally far; BFS neighbor order reaches (6,1) first.
        assertEquals(cell(6, 1), map.findBombTarget(0, 0, setOf(cell(5, 0))))
    }

    @Test
    fun `returns null when every brick-adjacent cell is excluded`() {
        val map = TestMaps.withBricks(cell(6, 0))
        val allAround = setOf(cell(5, 0), cell(7, 0), cell(6, 1))
        assertNull(map.findBombTarget(0, 0, allAround))
    }

    @Test
    fun `stays inside the region the hero can actually walk`() {
        // Hero sealed into (0,0), with a brick cluster far away.
        val map = TestMaps.withBricks(cell(1, 0), cell(0, 1), cell(20, 10), cell(21, 10))

        assertEquals(cell(0, 0), map.findBombTarget(0, 0, emptySet()))

        assertNull(map.findBombTarget(0, 0, setOf(cell(0, 0))))

        // findAnyBombTarget ignores reachability.
        val anywhere = map.findAnyBombTarget(0, 0, setOf(cell(0, 0)))
        assertNotNull(anywhere)
        assertTrue(map.hasBlockAround(anywhere.i, anywhere.j))
    }

    @Test
    fun `findAnyBombTarget reaches cells the walking BFS cannot`() {
        // Block-pass hero standing on a brick surrounded by bricks.
        val map = TestMaps.fromAscii(
            "BBB",
            "B#B",
            "BBB",
        )
        assertNull(map.findBombTarget(1, 0, emptySet()), "the walking BFS cannot leave a brick cell")

        val anywhere = map.findAnyBombTarget(1, 0, emptySet())
        assertNotNull(anywhere, "a sealed-in hero must still be given somewhere to go")
        assertTrue(map.canSetBoom(anywhere.i, anywhere.j))
        assertTrue(map.hasBlockAround(anywhere.i, anywhere.j))
    }

    @Test
    fun `findAnyBombTarget picks the nearest by manhattan distance`() {
        val map = TestMaps.withBricks(cell(4, 0), cell(30, 16))
        val target = map.findAnyBombTarget(30, 15, emptySet())
        assertNotNull(target)
        assertTrue(
            manhattan(target, cell(30, 15)) <= manhattan(cell(3, 0), cell(30, 15)),
            "should prefer the brick next door, not the one across the map",
        )
    }

    @Test
    fun `never picks a fixed wall or an out-of-bounds cell`() {
        val map = TestMaps.withBricks(cell(1, 2), cell(2, 1))
        val target = map.findBombTarget(0, 0, emptySet())
        assertNotNull(target)
        assertTrue(target.i in 0 until MAP_MAX_COL && target.j in 0 until MAP_MAX_ROW)
        assertTrue(!(target.i % 2 == 1 && target.j % 2 == 1), "(${target.i},${target.j}) is a fixed wall")
    }

    @Test
    fun `never picks a cell that holds a brick`() {
        val map = TestMaps.withBricks(cell(2, 0), cell(3, 0))
        val target = map.findBombTarget(0, 0, emptySet())
        assertNotNull(target)
        assertEquals(cell(1, 0), target)
        assertTrue(map.canSetBoom(target.i, target.j))
    }
}
