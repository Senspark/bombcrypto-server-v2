package com.senspark.client.explodev6

import com.senspark.game.controller.MapData
import com.senspark.game.data.model.user.BlockMap
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.GameConstants
import com.senspark.game.declare.GameConstants.MAP_MAX_COL
import com.senspark.game.declare.GameConstants.MAP_MAX_ROW

// (col, row) == (i, j)
typealias Cell = Pair<Int, Int>

val Cell.i get() = first
val Cell.j get() = second

fun cell(i: Int, j: Int): Cell = i to j

fun manhattan(a: Cell, b: Cell): Int = Math.abs(a.i - b.i) + Math.abs(a.j - b.j)

// Deterministic [MapData] fixtures. Fixed walls (odd i and odd j) come from MapData itself.
object TestMaps {
    const val WALL = '#'
    const val BRICK = 'B'
    const val EMPTY = '.'

    fun empty(): MapData = MapData().also { it.mode = EnumConstants.MODE.PVE_V2 }

    fun withBricks(vararg cells: Cell): MapData {
        val map = empty()
        for (c in cells) {
            map.addBlockMap(brick(c.i, c.j))
        }
        return map
    }

    fun withBlocks(vararg blocks: BlockMap): MapData {
        val map = empty()
        for (b in blocks) map.addBlockMap(b)
        return map
    }

    // Top-left corner as ASCII, rows in j order: `.` empty, `B` brick, `#` wall (must match MapData).
    fun fromAscii(vararg rows: String): MapData {
        require(rows.size <= MAP_MAX_ROW) { "map is only $MAP_MAX_ROW rows tall" }
        val map = empty()
        for (j in rows.indices) {
            val row = rows[j]
            require(row.length <= MAP_MAX_COL) { "map is only $MAP_MAX_COL columns wide" }
            for (i in row.indices) {
                val isWall = i % 2 == 1 && j % 2 == 1
                when (row[i]) {
                    WALL -> require(isWall) { "($i,$j) is drawn as a wall but MapData has no wall there" }
                    BRICK -> {
                        require(!isWall) { "($i,$j) is a fixed wall, it cannot hold a brick" }
                        map.addBlockMap(brick(i, j))
                    }
                    EMPTY -> require(!isWall) { "($i,$j) is a fixed wall, draw it as '#'" }
                    else -> throw IllegalArgumentException("unknown map character '${row[i]}' at ($i,$j)")
                }
            }
        }
        return map
    }

    fun brick(i: Int, j: Int, hp: Int = 1, maxHp: Int = hp): BlockMap =
        BlockMap(i, j, GameConstants.BLOCK_TYPE.NORMAL, hp, maxHp)
}
