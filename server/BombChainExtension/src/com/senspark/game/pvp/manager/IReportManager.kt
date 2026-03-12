package com.senspark.game.pvp.manager

import com.senspark.common.pvp.IMatchResultInfo
import com.senspark.game.pvp.data.IMatchObserveData
import com.senspark.game.pvp.data.IMatchStartData
import com.senspark.game.pvp.info.IMatchHistoryInfo

interface IReportManager {
    val info: IMatchHistoryInfo
    fun start(data: IMatchStartData)
    fun observe(data: IMatchObserveData)
    fun finish(info: IMatchResultInfo)
}