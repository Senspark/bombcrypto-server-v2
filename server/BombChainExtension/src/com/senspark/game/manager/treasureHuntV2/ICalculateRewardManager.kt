package com.senspark.game.manager.treasureHuntV2

import com.senspark.game.data.model.config.RewardLevelConfig
import com.senspark.game.data.model.config.TreasureHuntV2Config
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.declare.EnumConstants.DataType

interface ICalculateRewardManager {
    fun setConfig(config: TreasureHuntV2Config, rewardLevelConfig: Map<Int, RewardLevelConfig>)
    fun calculateReward(): Map<UserId, List<RewardResult>>
    fun addHeroToPool(hero: Hero, userId: UserId, raceId: Int)
}

data class UserId(
    val userId: Int,
    val userName: String,
    val dataType: DataType,
)