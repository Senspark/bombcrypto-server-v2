package com.senspark.game.pvp.info

import com.senspark.common.pvp.IMatchUserInfo
import com.senspark.game.pvp.data.IMatchObserveData
import com.senspark.game.pvp.data.IMatchStartData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class MatchHistoryInfo(
    @SerialName("id") override val id: String,
    @SerialName("start_timestamp") override val startTimestamp: Long,
    @SerialName("user_info") override val userInfo: List<IMatchUserInfo>,
    @SerialName("start_data") override val startData: IMatchStartData,
    @SerialName("observe_data") override val observeData: List<IMatchObserveData>
) : IMatchHistoryInfo