package com.senspark.game.pvp.strategy.map

import com.senspark.game.pvp.entity.Direction
import com.senspark.game.pvp.entity.IBomb
import com.senspark.game.pvp.manager.IBombManager
import com.senspark.game.pvp.manager.IMapManager
import kotlin.math.max

private class ExpandResult(
    override val damagedPositions: Map<Pair<Int, Int>, Int>,
    override val explodedBombs: List<IBomb>,
) : IExpandResult

class InstantExpandStrategy : IExpandStrategy {
    override fun expand(
        bombManager: IBombManager,
        mapManager: IMapManager,
        bomb: IBomb,
    ): IExpandResult {
        val bombSet = mutableSetOf(bomb)
        val bombs = mutableListOf(bomb)
        val damagedPositions = mutableMapOf<Pair<Int, Int>, Int>()
        // Process the specified position.
        val processPosition: (Int, Int, Int) -> Unit = { xx, yy, damage ->
            val affectedBomb = bombManager.getBomb(xx, yy)
            if (affectedBomb != null && !bombSet.contains(affectedBomb)) {
                // Add to processing bombs.
                bombs.add(affectedBomb)
                bombSet.add(affectedBomb)
            }
            // Take damage.
            damagedPositions.compute(xx to yy) { _, oldDamage ->
                if (oldDamage == null) {
                    damage
                } else {
                    max(oldDamage, damage)
                }
            }
        }
        // Expansion.
        var index = 0;
        while (index < bombs.size) {
            val item = bombs[index++]
            val x = item.x.toInt()
            val y = item.y.toInt()
            val ranges = item.state.explodeRanges
            val damage = item.damage
            // Process the current position.
            processPosition(x, y, damage)
            // Expand horizontal directions.
            // Left.
            for (xx in x - ranges.getValue(Direction.Left) until x) {
                processPosition(xx, y, damage)
            }
            // Right.
            for (xx in x + ranges.getValue(Direction.Right) downTo x + 1) {
                processPosition(xx, y, damage)
            }
            // Expand vertical directions.
            // Up.
            for (yy in y + ranges.getValue(Direction.Up) downTo y + 1) {
                processPosition(x, yy, damage)
            }
            // Down.
            for (yy in y - ranges.getValue(Direction.Down) until y) {
                processPosition(x, yy, damage)
            }
        }
        return ExpandResult(damagedPositions, bombs)
    }
}