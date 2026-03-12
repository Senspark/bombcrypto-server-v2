package com.senspark.game.data.manager.adventure

import com.senspark.common.service.IServerService
import com.senspark.game.data.model.config.ReviveHeroCost

interface IAdventureReviveHeroCostManager : IServerService {
    fun get(times: Int): ReviveHeroCost
    fun getNextTimeCost(currentTimes: Int): ReviveHeroCost?
}