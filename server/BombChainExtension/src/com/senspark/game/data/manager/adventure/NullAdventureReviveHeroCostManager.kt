package com.senspark.game.data.manager.adventure

import com.senspark.game.data.model.config.ReviveHeroCost
import com.senspark.game.exception.CustomException

class NullAdventureReviveHeroCostManager : IAdventureReviveHeroCostManager {

    override fun initialize() {
    }

    override fun get(times: Int): ReviveHeroCost {
        throw CustomException("Feature not support")
    }

    override fun getNextTimeCost(currentTimes: Int): ReviveHeroCost? {
        return null
    }
}