package com.senspark.game.pvp.data

import com.senspark.common.pvp.IMatchData
import com.senspark.game.pvp.info.IMapInfo

interface IMatchStartData {
    val matchData: IMatchData
    val mapInfo: IMapInfo
}