package com.senspark.game.handler.gacha

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class StartOpeningGachaChestHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.START_OPENING_GACHA_CHEST_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val chestId = data.getInt("chest_id")
            val remainingTime = controller.masterUserManager.userGachaChestManager.startOpeningGachaChest(chestId)
            val response = SFSObject.newInstance().apply { putLong("remaining_time", remainingTime) }
            sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }
}