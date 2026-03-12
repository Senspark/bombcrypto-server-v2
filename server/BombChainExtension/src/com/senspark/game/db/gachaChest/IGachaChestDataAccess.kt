package com.senspark.game.db.gachaChest

import com.senspark.common.service.IGlobalService
import com.senspark.game.data.model.user.UserGachaChest
import com.senspark.game.declare.customEnum.GachaChestType
import com.senspark.game.user.IGachaChestManager

interface IGachaChestDataAccess : IGlobalService {
    fun addGachaChestForUser(
        uid: Int,
        chestType: GachaChestType,
        currentUserChestSlot: Int,
        gachaChestManager: IGachaChestManager,
        isCanOpenNow: Boolean
    ): UserGachaChest?

    fun getUserGachaChests(uid: Int, gachaChestManager: IGachaChestManager): List<UserGachaChest>


    fun skipOpenTimeByAds(
        uid: Int,
        chest: UserGachaChest,
        skipTimeInMilli: Long
    ): UserGachaChest

    fun startOpeningGachaChest(userGachaChest: UserGachaChest)

    fun skipOpenTimeByGem(
        uid: Int,
        chestId: Int,
        gemPrice: Int
    )
}