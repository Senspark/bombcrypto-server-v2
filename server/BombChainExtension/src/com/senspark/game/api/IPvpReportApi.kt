package com.senspark.game.api

import com.senspark.game.pvp.info.IMatchHistoryInfo

interface IPvpReportApi {
    fun report(info: IMatchHistoryInfo)
}