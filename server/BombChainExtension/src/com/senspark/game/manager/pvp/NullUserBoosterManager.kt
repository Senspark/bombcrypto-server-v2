package com.senspark.game.manager.pvp

import com.senspark.game.data.model.user.UserBooster
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray

class NullUserBoosterManager : IUserBoosterManager {
    override fun loadFromDb() {}

    override fun getBooster(itemId: Int): UserBooster? {
        return null
    }

    @Throws(Exception::class)
    override fun chooseBooster(itemId: Int, chosen: Boolean) {
    }

    override fun usePvpBooster(boosterValues: List<Int>, isWhiteList: Boolean) {}

    override fun toSfsArray(): ISFSArray {
        return SFSArray()
    }

    override fun getSelectBoosters(): List<UserBooster> {
        return emptyList()
    }

    override fun saveUsedBooster() {}
}