package com.senspark.game.data.manager.stake

import com.senspark.game.data.model.config.StakeVipReward

class NullStakeVipRewardManager : IStakeVipRewardManager {
    override val rewards: Map<Int, List<StakeVipReward>> get() = emptyMap()

    override fun initialize() {
    }
} 
