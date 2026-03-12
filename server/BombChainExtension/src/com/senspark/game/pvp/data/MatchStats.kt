package com.senspark.game.pvp.data

import com.senspark.common.pvp.IMatchStats
import com.senspark.common.pvp.IMatchUserStats
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class MatchStats(
    @SerialName("user_stats") override val userStats: List<IMatchUserStats>
) : IMatchStats