package com.senspark.game.pvp

import com.senspark.common.service.IServerService
import com.senspark.game.api.IPvpResultInfo

interface IPvpMatchReward {
    val rewardId: String
    val isOutOfChestSlot: Boolean
}

interface IPvpResultManager : IServerService {
    fun claimReward(userId: Int): IPvpMatchReward?
    fun handleResult(info: IPvpResultInfo)
}