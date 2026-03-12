package com.senspark.game.pvp.strategy.rank

import com.senspark.common.data.IBombRank
import com.senspark.common.pvp.IRankManager
import com.senspark.common.pvp.IRankResult
import com.senspark.game.constant.Booster
import com.senspark.game.pvp.data.RankResult

class SeasonRankStrategy(
    private val _rankManager: IRankManager,
    private val _data: List<IBombRank>,
) : IRankStrategy {
    /** Booster modifiers. */
    private val _winBoosters = mapOf(
        Booster.FullConquestCard to 1f,
        Booster.ConquestCard to .5f
    )

    private val _loseBoosters = mapOf(
        Booster.FullRankGuardian to -1f,
        Booster.RankGuardian to -.5f
    )

    override fun calculate(
        isDraw: Boolean,
        isWinner: Boolean,
        slot: Int,
        boosters: List<Int>,
        points: List<Int>,
    ): IRankResult {
        var deltaPoint = 0
        val usedBoosters = mutableListOf<Int>()
        val usableBoosters: Map<Booster, Float>
        val ranks = points.map { _rankManager.getRank(it) }
        val rank = ranks[slot]
        if (isDraw) {
            // No change.
            usableBoosters = emptyMap()
        } else if (isWinner) {
            // Winner.
            // Min rank of losers.
            val loserRank = (points.indices)
                .filter { it != slot }
                .minOf { ranks[it] }
            if (rank - loserRank >= 3) {
                // High rank delta.
                deltaPoint += 1
                usableBoosters = emptyMap()
            } else {
                deltaPoint = _data.find { it.bombRank == rank }?.winPoint ?: throw Exception("Could not find win bonus for rank=$rank")
                usableBoosters = _winBoosters
            }
        } else {
            // Loser.
            deltaPoint = _data.find { it.bombRank == rank }?.loosePoint ?: throw Exception("Could not find loose bonus for rank=$rank")
            usableBoosters = _loseBoosters
        }
        usableBoosters.forEach {
            if (boosters.contains(it.key.value)) {
                deltaPoint += (deltaPoint * it.value).toInt()
                usedBoosters.add(it.key.value)
                return@forEach // Use the first one.
            }
        }
        val finalPoint = points[slot] + deltaPoint
        val finalRank = _rankManager.getRank(finalPoint)
        return RankResult(
            rank = finalRank,
            point = finalPoint,
            deltaPoint = deltaPoint,
            usedBoosters = usedBoosters,
        )
    }
}