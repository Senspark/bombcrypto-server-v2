package com.senspark.mapservice.auto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PathfinderTest {
    private fun open(blocked: Set<Cell> = emptySet()): (Int, Int) -> Boolean =
        { i, j -> !(i % 2 == 1 && j % 2 == 1) && (i to j) !in blocked }

    @Test
    fun `ties break in the client's neighbour order`() {
        // Two equal routes around the (1,1) wall; BFS.cs visits i-1, i+1, j-1, j+1 so the i-first one wins.
        assertEquals(listOf(1 to 0, 2 to 0, 2 to 1, 2 to 2), Pathfinder.shortestPath(open(), 0 to 0, 2 to 2))
    }

    @Test
    fun `blocked destination or no route is null, same cell is empty`() {
        assertNull(Pathfinder.shortestPath(open(setOf(2 to 0)), 0 to 0, 2 to 0))
        assertNull(Pathfinder.shortestPath(open(setOf(1 to 0, 0 to 1)), 0 to 0, 4 to 0))
        assertEquals(emptyList(), Pathfinder.shortestPath(open(), 3 to 4, 3 to 4))
    }

    @Test
    fun `source may be blocked (a hero standing on its own bomb walks off it)`() {
        assertEquals(listOf(1 to 0), Pathfinder.shortestPath(open(setOf(0 to 0)), 0 to 0, 1 to 0))
    }
}
