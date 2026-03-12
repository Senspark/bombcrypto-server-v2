package com.senspark.game.data.manager.stake

import com.senspark.common.service.IServerService
import com.senspark.game.data.model.config.StakeVipReward

interface IStakeVipRewardManager : IServerService {
    val rewards: Map<Int, List<StakeVipReward>>
}