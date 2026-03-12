package com.senspark.game.pvp.manager

import com.senspark.common.pvp.IMatchStats
import com.senspark.game.api.IPvpResultInfo

interface IDatabaseManager {
    fun addMatch(info: IPvpResultInfo, stats: IMatchStats)
}