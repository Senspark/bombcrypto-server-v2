package com.senspark.game.data.manager.stake

import com.senspark.game.data.model.config.StakeVipReward
import com.senspark.game.db.IShopDataAccess

class StakeVipRewardManager(
    private val _shopDataAccess: IShopDataAccess,
) : IStakeVipRewardManager {
    override val rewards: MutableMap<Int, List<StakeVipReward>> = mutableMapOf()

    override fun initialize() {
        rewards.putAll(_shopDataAccess.loadStakeVipRewards())
    }
} 
