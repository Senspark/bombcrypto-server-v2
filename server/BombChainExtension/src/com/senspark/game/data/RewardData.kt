package com.senspark.game.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class RewardData(
    val id: Int,
    @SerialName("reward_type")
    val rewardType: String, // FIXME: remove.
    val type: String,
    val value: Float
)