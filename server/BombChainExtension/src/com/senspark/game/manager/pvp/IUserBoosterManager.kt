package com.senspark.game.manager.pvp

import com.senspark.game.data.model.user.UserBooster
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.ISFSArray

interface IUserBoosterManager {
    fun loadFromDb()
    fun getBooster(itemId: Int): UserBooster?

    @Throws(Exception::class)
    fun chooseBooster(itemId: Int, chosen: Boolean)

    @Throws(CustomException::class)
    fun usePvpBooster(boosterValues: List<Int>, isWhiteList: Boolean)
    fun toSfsArray(): ISFSArray
    fun getSelectBoosters(): List<UserBooster>
    fun saveUsedBooster()
}