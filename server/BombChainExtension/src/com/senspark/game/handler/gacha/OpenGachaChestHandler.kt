package com.senspark.game.handler.gacha

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class OpenGachaChestHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.OPEN_GACHA_CHEST_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val gachaChestManager = controller.masterUserManager.userGachaChestManager
            val chestId = data.getInt("chest_id")
            val response = SFSObject().apply {
                putSFSArray("data", gachaChestManager.openChest(chestId))
            }
            sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }
}