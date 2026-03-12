package com.senspark.game.pvp.data

import com.senspark.common.pvp.IRankResult

class RankResult(
    override val rank: Int,
    override val point: Int,
    override val deltaPoint: Int,
    override val usedBoosters: List<Int>,
) : IRankResult