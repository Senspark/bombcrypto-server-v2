package com.senspark.game.handler.marketplace

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class GetActivityHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_ACTIVITY_MARKETPLACE_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val result = controller.masterUserManager.userMarketplaceManager.getActivity()
            val response = SFSObject().apply { putSFSArray("data", result) }
            return sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }
}