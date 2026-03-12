package com.senspark.game.pvp.strategy.rank

import com.senspark.common.pvp.IRankResult

interface IRankStrategy {
    fun calculate(
        isDraw: Boolean,
        isWinner: Boolean,
        slot: Int,
        boosters: List<Int>,
        points: List<Int>,
    ): IRankResult
}