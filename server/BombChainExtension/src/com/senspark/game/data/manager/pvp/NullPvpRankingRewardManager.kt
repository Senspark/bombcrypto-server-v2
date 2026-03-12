package com.senspark.game.data.manager.pvp

import com.senspark.game.data.model.config.UserPvpRankingReward
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray

class NullPvpRankingRewardManager : IPvpRankingRewardManager {

    override fun initialize() {
    }
    
    override fun setData(data: Map<Int, UserPvpRankingReward>) {}

    override fun getConfigPvpRankingReward(): ISFSArray {
        return SFSArray()
    }

    override fun reload() {}

    override fun getReward(userId: Int): UserPvpRankingReward {
        throw CustomException("Feature not support")
    }
}