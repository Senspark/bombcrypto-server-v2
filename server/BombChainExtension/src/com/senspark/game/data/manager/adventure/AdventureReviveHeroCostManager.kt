package com.senspark.game.data.manager.adventure

import com.senspark.game.data.model.config.ReviveHeroCost
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.exception.CustomException

class AdventureReviveHeroCostManager(
    val shopDataAccess: IShopDataAccess,
) : IAdventureReviveHeroCostManager {

    private val reviveHeroCosts: MutableMap<Int, ReviveHeroCost> = mutableMapOf()

    override fun initialize() {
        reviveHeroCosts.putAll(shopDataAccess.loadReviveHeroCosts())
    }

    override fun get(times: Int): ReviveHeroCost {
        return reviveHeroCosts[times] ?: throw CustomException("Hero cannot revive again")
    }

    override fun getNextTimeCost(currentTimes: Int): ReviveHeroCost? {
        return reviveHeroCosts[currentTimes + 1]
    }
}