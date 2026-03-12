package com.senspark.game.pvp.data

import com.senspark.game.pvp.info.IFallingBlockInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class FallingBlockData(
    @SerialName("match_id") override val matchId: String,
    @SerialName("blocks") override val blocks: List<IFallingBlockInfo>
) : IFallingBlockData