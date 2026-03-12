package com.senspark.game.handler.request

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class GetSkinInventoryHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_SKIN_INVENTORY_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        val response = SFSObject()
        try {
            response.putSFSArray(
                "userInventoryNFT",
                controller.masterUserManager.userInventoryManager.getInventoryToSFSArray()
            )
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
        return sendSuccess(controller, requestId, response)
    }
}