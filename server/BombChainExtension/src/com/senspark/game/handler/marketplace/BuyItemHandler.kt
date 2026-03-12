package com.senspark.game.handler.marketplace

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.exception.CustomException
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

// Xoá handler này trong nhũng version tới
class BuyItemHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.BUY_ITEM_MARKETPLACE_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            throw CustomException("Please use new version to access marketplace")
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }
}