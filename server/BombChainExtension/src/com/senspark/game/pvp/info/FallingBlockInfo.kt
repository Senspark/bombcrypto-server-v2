package com.senspark.game.pvp.info

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class FallingBlockInfo(
    @SerialName("timestamp") override val timestamp: Int,
    @SerialName("x") override val x: Int,
    @SerialName("y") override val y: Int
) : IFallingBlockInfo