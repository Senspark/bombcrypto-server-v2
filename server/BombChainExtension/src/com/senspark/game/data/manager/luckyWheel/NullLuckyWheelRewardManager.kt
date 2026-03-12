package com.senspark.game.data.manager.luckyWheel

import com.senspark.game.data.model.config.Item
import com.senspark.game.data.model.config.LuckyWheelReward
import com.senspark.game.exception.CustomException

class NullLuckyWheelRewardManager : ILuckyWheelRewardManager {

    override val rewards: List<LuckyWheelReward> get() = emptyList()

    override fun initialize() {
    }

    override fun randomReward(): Pair<LuckyWheelReward, List<Triple<Item, Int, Long>>> {
        throw CustomException("Feature not support")
    }
}