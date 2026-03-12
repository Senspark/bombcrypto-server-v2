package com.senspark.game.handler.newMarket

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class CancelOrderItemMarketHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.CANCEL_ORDER_ITEM_MARKET_V3

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val isCancelSuccess = controller.masterUserManager.userMarketplaceManager.cancelOrder(controller.userId)
            if (!isCancelSuccess) {
                throw Exception("User ${controller.userId} Cancel order failed")
            }

            return sendSuccess(
                controller,
                requestId,
                SFSObject()
            )
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }
}