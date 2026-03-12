package com.senspark.game.pvp.data

import com.senspark.common.pvp.IMatchData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class MatchFinishData(
    @SerialName("match") override val matchData: IMatchData,
) : IMatchFinishData 