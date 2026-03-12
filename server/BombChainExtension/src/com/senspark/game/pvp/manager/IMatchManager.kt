package com.senspark.game.pvp.manager

import com.senspark.common.pvp.IMatchInfo
import com.senspark.common.pvp.IMatchStats
import com.senspark.game.api.IPvpResultInfo
import com.senspark.game.pvp.info.IMatchHistoryInfo
import com.smartfoxserver.v2.entities.User

interface IMatchManager {
    /** Validates the specified match info. */
    fun validate(info: IMatchInfo, hash: String)

    fun join(user: User)
    fun leave(user: User)
    fun finish(resultInfo: IPvpResultInfo, historyInfo: IMatchHistoryInfo, stats: IMatchStats)
}