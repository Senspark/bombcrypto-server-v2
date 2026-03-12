package com.senspark.game.pvp.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class MatchReadyData(
    @SerialName("match_id") override val matchId: String,
    @SerialName("slot") override val slot: Int
) : IMatchReadyData