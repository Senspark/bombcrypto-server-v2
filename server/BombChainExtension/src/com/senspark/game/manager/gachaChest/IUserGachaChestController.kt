package com.senspark.game.manager.gachaChest

import com.senspark.game.controller.IUserController
import com.senspark.game.data.model.user.UserGachaChest
import com.senspark.game.db.model.UserGachaChestSlot
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.customEnum.GachaChestType
import com.smartfoxserver.v2.entities.data.ISFSArray

interface IUserGachaChestController {
    fun getAllChests(): List<UserGachaChestSlot>
    fun addChestFromBlockRewardType(blockRewardType: EnumConstants.BLOCK_REWARD_TYPE): UserGachaChest?
    fun buyAndOpenGachaChest(
        chestType: GachaChestType,
        quantity: Int,
        rewardType: EnumConstants.BLOCK_REWARD_TYPE,
        controller: IUserController
    ): ISFSArray

    /**
     * start countdown to open chest
     */
    fun startOpeningGachaChest(chestId: Int): Long
    fun openChest(id: Int): ISFSArray
    fun skipOpenTimeByGem(chestId: Int)
    suspend fun skipOpenTimeByAds(chestId: Int, token: String): Long
}