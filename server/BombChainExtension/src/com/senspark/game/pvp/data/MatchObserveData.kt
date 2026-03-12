package com.senspark.game.pvp.data

import com.senspark.game.pvp.delta.IBlockStateDelta
import com.senspark.game.pvp.delta.IBombStateDelta
import com.senspark.game.pvp.delta.IHeroStateDelta
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class MatchObserveData(
    @SerialName("id") override val id: Int,
    @SerialName("timestamp") override val timestamp: Long,
    @SerialName("match_id") override val matchId: String,
    @SerialName("heroes") override val heroDelta: List<IHeroStateDelta>,
    @SerialName("bombs") override val bombDelta: List<IBombStateDelta>,
    @SerialName("blocks") override val blockDelta: List<IBlockStateDelta>,
) : IMatchObserveData