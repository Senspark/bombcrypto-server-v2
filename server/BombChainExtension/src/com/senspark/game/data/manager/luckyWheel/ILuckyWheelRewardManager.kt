package com.senspark.game.data.manager.luckyWheel

import com.senspark.common.service.IServerService
import com.senspark.game.data.model.config.Item
import com.senspark.game.data.model.config.LuckyWheelReward

interface ILuckyWheelRewardManager : IServerService {
    val rewards: List<LuckyWheelReward>
    fun randomReward(): Pair<LuckyWheelReward, List<Triple<Item, Int, Long>>>
} 