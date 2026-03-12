package com.senspark.game.handler.gacha

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.customEnum.GachaChestType
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class BuyGachaChestHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.BUY_GACHA_CHEST_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val manager = controller.masterUserManager.userGachaChestManager
            controller.saveGameAndLoadReward()
            val chestType = GachaChestType.fromValue(data.getInt("chest_type"))
            val rewardType = if (data.containsKey("reward_type")) {
                BLOCK_REWARD_TYPE.valueOf(data.getInt("reward_type"))
            } else BLOCK_REWARD_TYPE.GEM
            val quantity = if (data.containsKey("quantity")) data.getInt("quantity") else 1
            val response = manager.buyAndOpenGachaChest(chestType, quantity, rewardType, controller)
            sendSuccess(controller, requestId, SFSObject().apply { putSFSArray("data", response) })
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }
}