package com.senspark.mapservice.auto

import com.senspark.mapservice.domain.GameConstants

typealias Cell = Pair<Int, Int>

// Port of the client's DefaultMapManagerV2.ShortestPath + BFS.cs: same neighbour order (i-1, i+1, j-1, j+1);
// the source is always allowed (a hero walks off its own bomb), the destination must be passable.
object Pathfinder {
    private val DIRS = arrayOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

    /** Tiles to walk after [source] up to and including [dest]; empty if already there, null if unreachable. */
    fun shortestPath(passable: (Int, Int) -> Boolean, source: Cell, dest: Cell): List<Cell>? {
        if (source == dest) return emptyList()
        val cols = GameConstants.MAP_MAX_COL
        val rows = GameConstants.MAP_MAX_ROW
        if (dest.first !in 0 until cols || dest.second !in 0 until rows) return null
        if (!passable(dest.first, dest.second)) return null

        val prev = Array(cols) { IntArray(rows) { -1 } }
        val visited = Array(cols) { BooleanArray(rows) }
        val queue = IntArray(cols * rows)
        var head = 0
        var tail = 0
        visited[source.first][source.second] = true
        queue[tail++] = source.first * rows + source.second

        while (head < tail) {
            val node = queue[head++]
            val ci = node / rows
            val cj = node % rows
            if (ci == dest.first && cj == dest.second) {
                val path = ArrayList<Cell>()
                var cur = node
                while (cur != source.first * rows + source.second) {
                    path.add(cur / rows to cur % rows)
                    cur = prev[cur / rows][cur % rows]
                }
                path.reverse()
                return path
            }
            for ((di, dj) in DIRS) {
                val ni = ci + di
                val nj = cj + dj
                if (ni !in 0 until cols || nj !in 0 until rows) continue
                if (visited[ni][nj] || !passable(ni, nj)) continue
                visited[ni][nj] = true
                prev[ni][nj] = node
                queue[tail++] = ni * rows + nj
            }
        }
        return null
    }
}
