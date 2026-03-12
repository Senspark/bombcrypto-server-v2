package com.senspark.game.pvp.info

import com.senspark.common.pvp.IMatchUserInfo
import com.senspark.game.pvp.data.IMatchObserveData
import com.senspark.game.pvp.data.IMatchStartData

interface IMatchHistoryInfo {
    val id: String
    val startTimestamp: Long
    val userInfo: List<IMatchUserInfo>
    val startData: IMatchStartData
    val observeData: List<IMatchObserveData>
}