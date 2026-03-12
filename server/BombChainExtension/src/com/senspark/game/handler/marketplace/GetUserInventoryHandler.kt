package com.senspark.game.handler.marketplace

import com.senspark.game.controller.IUserController
import com.senspark.game.data.model.Filter
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class GetUserInventoryHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_DASHBOARD_MARKETPLACE_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val type = data.getInt("type")
            val manager = controller.masterUserManager.userMarketplaceManager
            val response = manager.getUserInventoryToSFSArray(Filter(type = type))
            return sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }
}