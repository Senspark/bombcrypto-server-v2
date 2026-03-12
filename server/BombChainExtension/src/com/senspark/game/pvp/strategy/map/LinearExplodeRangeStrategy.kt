package com.senspark.game.pvp.strategy.map

import com.senspark.game.pvp.entity.BlockType
import com.senspark.game.pvp.entity.Direction
import com.senspark.game.pvp.entity.isBlock
import com.senspark.game.pvp.manager.IMapManager

class LinearExplodeRangeStrategy : IExplodeRangeStrategy {
    override fun getExplodeRange(
        manager: IMapManager,
        x: Int,
        y: Int,
        range: Int,
        piercing: Boolean,
        direction: Direction,
    ): Int {
        // Left, right, up, down.
        // Returns: should increase range/should process next range.
        val processPosition: (Int, Int) -> Pair<Boolean, Boolean> = processPosition@{ xx, yy ->
            val block = manager.getBlock(xx, yy)
            if (block != null) {
                if (block.type == BlockType.Hard) {
                    // Blocked by a hard block.
                    // No damage.
                    return@processPosition Pair(false, false)
                }
                if (block.isBlock) {
                    // Blocked by a normal block.
                    // Still damaged.
                    return@processPosition Pair(true, piercing)
                }
            }
            // Pass through.
            return@processPosition Pair(true, true)
        }
        val (xStep, yStep) = when (direction) {
            Direction.Left -> Pair(-1, 0)
            Direction.Right -> Pair(+1, 0)
            Direction.Up -> Pair(0, +1)
            Direction.Down -> Pair(0, -1)
        }
        var currentRange = 0
        while (currentRange < range) {
            val xx = x + xStep * (currentRange + 1)
            val yy = y + yStep * (currentRange + 1)
            if (xx in 0 until manager.width &&
                yy in 0 until manager.height) {
                val (shouldIncreaseRange, shouldProcess) = processPosition(xx, yy)
                if (shouldIncreaseRange) {
                    ++currentRange
                }
                if (shouldProcess) {
                    continue
                }
            }
            break
        }
        return currentRange
    }
}