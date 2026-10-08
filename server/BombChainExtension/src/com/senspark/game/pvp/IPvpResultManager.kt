package com.senspark.game.pvp

import com.senspark.common.service.IServerService
import com.senspark.game.api.IPvpResultInfo

interface IPvpMatchReward {
    val rewardId: String
    val isOutOfChestSlot: Boolean
    val hasHeroCage: Boolean
}

interface IPvpResultManager : IServerService {
    suspend fun claimReward(userId: Int): IPvpMatchReward?
    fun handleResult(info: IPvpResultInfo)
}