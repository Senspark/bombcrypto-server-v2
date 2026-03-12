package com.senspark.game.pvp.strategy.rank

import com.senspark.common.pvp.IRankManager
import com.senspark.common.pvp.IRankResult
import com.senspark.game.pvp.data.RankResult

class CasualRankStrategy(
    private val _rankManager: IRankManager,
) : IRankStrategy {
    private val _usableBoosters = emptySet<Int>(
        // Disable all intro boosters.
    )

    override fun calculate(
        isDraw: Boolean,
        isWinner: Boolean,
        slot: Int,
        boosters: List<Int>,
        points: List<Int>,
    ): IRankResult {
        val usedBoosters = boosters.filter {
            _usableBoosters.contains(it)
        }
        val finalPoint = points[slot]
        val finalRank = _rankManager.getRank(finalPoint)
        return RankResult(
            rank = finalRank,
            point = finalPoint,
            deltaPoint = 0,
            usedBoosters = usedBoosters,
        )
    }
}