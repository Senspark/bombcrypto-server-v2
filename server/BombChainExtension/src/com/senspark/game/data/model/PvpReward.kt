package com.senspark.game.data.model

import com.senspark.game.declare.EnumConstants
import kotlinx.serialization.Serializable

@Serializable
class PvpReward(
    val gem: Int
) {
    fun parseReward(): Map<EnumConstants.BLOCK_REWARD_TYPE, Float> {
        return mapOf(
            EnumConstants.BLOCK_REWARD_TYPE.GEM_LOCKED to gem.toFloat()
        )
    }
}