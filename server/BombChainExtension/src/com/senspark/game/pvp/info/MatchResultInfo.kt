package com.senspark.game.pvp.info

import com.senspark.common.pvp.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class MatchResultInfo(
    @SerialName("is_draw") override val isDraw: Boolean,
    @SerialName("winning_team") override val winningTeam: Int,
    @SerialName("scores") override val scores: List<Int>,
    @SerialName("duration") override val duration: Int,
    @SerialName("start_timestamp") override val startTimestamp: Long,
    @SerialName("info") override val info: List<IMatchResultUserInfo>,
) : IMatchResultInfo