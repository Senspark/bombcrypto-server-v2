package com.senspark.game.handler.newMarket

import com.senspark.game.api.OrderDataRequest
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class OrderItemMarketHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.ORDER_ITEM_MARKET_V3

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val itemId = data.getInt("item_id")
            val quantity = data.getInt("quantity")
            val expiration = data.getInt("expiration")
    
            val orderData = OrderDataRequest(
                buyerUid = controller.userId,
                itemId = itemId,
                quantity = quantity,
                expiration = expiration,
                
            )
            val response = controller.masterUserManager.userMarketplaceManager.order(orderData)

            return sendSuccess(
                controller,
                requestId,
                response
            )
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }
}