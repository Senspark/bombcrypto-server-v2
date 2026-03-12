package com.senspark.game.manager.gachaChest

import com.senspark.game.controller.IUserController
import com.senspark.game.data.model.user.UserGachaChest
import com.senspark.game.db.model.UserGachaChestSlot
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.customEnum.GachaChestType
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray

class NullUserGachaChestController : IUserGachaChestController {
    override fun buyAndOpenGachaChest(
        chestType: GachaChestType,
        quantity: Int,
        rewardType: BLOCK_REWARD_TYPE,
        controller: IUserController
    ): ISFSArray {
        return SFSArray()
    }

    override fun startOpeningGachaChest(chestId: Int): Long {
        return 0L
    }

    override fun openChest(id: Int): ISFSArray {
        return SFSArray()
    }

    override fun getAllChests(): List<UserGachaChestSlot> {
        return emptyList()
    }

    override fun addChestFromBlockRewardType(blockRewardType: BLOCK_REWARD_TYPE): UserGachaChest? {
        return null
    }

    override fun skipOpenTimeByGem(chestId: Int) {}

    override suspend fun skipOpenTimeByAds(chestId: Int, token: String): Long {
        return 0L
    }
}