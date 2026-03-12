package com.senspark.game.pvp.data

import com.senspark.common.pvp.IMatchData
import com.senspark.game.pvp.info.IMapInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class MatchStartData(
    @SerialName("match") override val matchData: IMatchData,
    @SerialName("map") override val mapInfo: IMapInfo,
) : IMatchStartData