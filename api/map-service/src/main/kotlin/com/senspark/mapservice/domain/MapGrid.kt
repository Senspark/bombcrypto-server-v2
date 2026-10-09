package com.senspark.mapservice.domain

import com.senspark.mapservice.model.BlockDto
import java.util.ArrayDeque

/** Mutable in-memory block, mirroring BombChainExtension's `BlockMap`. */
data class Block(
    val i: Int,
    val j: Int,
    val type: Int,
    var hp: Int,
    val maxHp: Int,
) {
    fun subHp(amount: Int) {
        hp -= amount
        if (hp <= 0) hp = 0
    }
}

/**
 * Ported from BombChainExtension's `com.senspark.game.controller.MapData`: the fixed odd/odd wall
 * grid, block lookup, BFS target-pick (`findBombTarget`), and the unreachable-fallback nearest-pick
 * (`findAnyBombTarget`). Map generation itself (`createRandomMap`/`reposition`) is NOT ported --
 * per the plan, the main server keeps owning procedural map generation (it needs DB-backed block
 * config/drop-rate data MapService never has) and just pushes the finished map here via `/map`.
 */
class MapGrid(
    var blocks: MutableList<Block>,
    var tileset: Int = 0,
    var mode: String = "PVE_V2",
) {
    private val wall = Array(GameConstants.MAP_MAX_COL) { i ->
        BooleanArray(GameConstants.MAP_MAX_ROW) { j -> i % 2 == 1 && j % 2 == 1 }
    }

    companion object {
        fun fromDtos(blocks: List<BlockDto>, tileset: Int, mode: String): MapGrid {
            return MapGrid(
                blocks.map { Block(it.i, it.j, it.type, it.hp, it.maxHp) }.toMutableList(),
                tileset,
                mode,
            )
        }
    }

    fun getBlock(i: Int, j: Int): Block? = blocks.firstOrNull { it.i == i && it.j == j }

    fun removeBlock(block: Block) {
        blocks.remove(block)
    }

    fun isEmpty(): Boolean = blocks.isEmpty()

    private fun truePosition(i: Int, j: Int): Boolean {
        return i in 0 until GameConstants.MAP_MAX_COL && j in 0 until GameConstants.MAP_MAX_ROW
    }

    private fun isBlockWall(i: Int, j: Int): Boolean = wall[i][j]

    private fun isContainBlock(i: Int, j: Int): Boolean = getBlock(i, j) != null

    fun inBounds(i: Int, j: Int): Boolean = truePosition(i, j)

    fun isWall(i: Int, j: Int): Boolean = truePosition(i, j) && wall[i][j]

    /** One pass over [blocks]; lets callers test many cells without a linear scan each. */
    fun blockGrid(): Array<BooleanArray> {
        val grid = Array(GameConstants.MAP_MAX_COL) { BooleanArray(GameConstants.MAP_MAX_ROW) }
        for (b in blocks) {
            if (truePosition(b.i, b.j)) grid[b.i][b.j] = true
        }
        return grid
    }

    /** Cells a hero can stand on: in bounds, not a wall, no block. */
    fun emptyCells(): List<Pair<Int, Int>> {
        val blocked = blockGrid()
        val result = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until GameConstants.MAP_MAX_COL) {
            for (j in 0 until GameConstants.MAP_MAX_ROW) {
                if (!wall[i][j] && !blocked[i][j]) result.add(i to j)
            }
        }
        return result
    }

    fun canSetBoom(i: Int, j: Int): Boolean {
        if (!truePosition(i, j)) return false
        if (isBlockWall(i, j)) return false
        if (isContainBlock(i, j)) return false
        return true
    }

    /** True if any of the 4 orthogonal neighbours of (i,j) holds a live block. */
    fun hasBlockAround(i: Int, j: Int): Boolean {
        return getBlock(i - 1, j) != null ||
            getBlock(i + 1, j) != null ||
            getBlock(i, j - 1) != null ||
            getBlock(i, j + 1) != null
    }

    /**
     * BFS from (fromI, fromJ) for the nearest reachable, brick-adjacent cell not in [excluded];
     * the seed itself qualifies. Null if nothing brick-adjacent is reachable right now.
     */
    fun findBombTarget(fromI: Int, fromJ: Int, excluded: Set<Pair<Int, Int>>): Pair<Int, Int>? {
        if (!truePosition(fromI, fromJ)) return null

        val visited = Array(GameConstants.MAP_MAX_COL) { BooleanArray(GameConstants.MAP_MAX_ROW) }
        val queue: ArrayDeque<Pair<Int, Int>> = ArrayDeque()
        visited[fromI][fromJ] = true
        queue.add(fromI to fromJ)

        while (queue.isNotEmpty()) {
            val (i, j) = queue.removeFirst()
            if (canSetBoom(i, j) && (i to j) !in excluded && hasBlockAround(i, j)) {
                return i to j
            }

            val neighbors = arrayOf(i - 1 to j, i + 1 to j, i to j - 1, i to j + 1)
            for ((ni, nj) in neighbors) {
                if (!truePosition(ni, nj) || visited[ni][nj]) continue
                if (isBlockWall(ni, nj) || isContainBlock(ni, nj)) continue
                visited[ni][nj] = true
                queue.add(ni to nj)
            }
        }
        return null
    }

    /**
     * Last resort for [findBombTarget]: nearest brick-adjacent cell anywhere on the map, ignoring
     * reachability. Deterministic -- callers must pass rejects in [excluded] to avoid repeats.
     */
    fun findAnyBombTarget(fromI: Int, fromJ: Int, excluded: Set<Pair<Int, Int>>): Pair<Int, Int>? {
        var best: Pair<Int, Int>? = null
        var bestDistance = Int.MAX_VALUE
        for (i in 0 until GameConstants.MAP_MAX_COL) {
            for (j in 0 until GameConstants.MAP_MAX_ROW) {
                if (!canSetBoom(i, j)) continue
                if ((i to j) in excluded) continue
                if (!hasBlockAround(i, j)) continue
                val distance = Math.abs(i - fromI) + Math.abs(j - fromJ)
                if (distance < bestDistance) {
                    bestDistance = distance
                    best = i to j
                }
            }
        }
        return best
    }
}
