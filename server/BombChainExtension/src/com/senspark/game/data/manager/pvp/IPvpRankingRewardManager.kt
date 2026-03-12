package com.senspark.game.data.manager.pvp

import com.senspark.common.service.IServerService
import com.senspark.game.data.model.config.UserPvpRankingReward
import com.smartfoxserver.v2.entities.data.ISFSArray

interface IPvpRankingRewardManager : IServerService {
    fun reload()
    fun getReward(userId: Int): UserPvpRankingReward
    fun setData(data: Map<Int, UserPvpRankingReward>)
    fun getConfigPvpRankingReward() : ISFSArray
} 