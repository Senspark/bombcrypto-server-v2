package com.senspark.game.handler.newMarket

import com.senspark.game.api.CancelDataRequest
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class CancelSellItemMarketHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.CANCEL_SELL_ITEM_MARKET_V3

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {

            val cancelData = CancelDataRequest(
                sellerUid = controller.userId,
                itemId = data.getInt("item_id"),
                price = data.getFloat("price"),
                itemType = data.getInt("item_type"),
                expiration = data.getInt("expiration")
            )

            val isSuccess = controller.masterUserManager.userMarketplaceManager.cancelV3(cancelData)
            
            if (!isSuccess) {
                throw Exception("User ${controller.userId} Cancel item failed")
            }
            return sendSuccess(
                controller,
                requestId,
                SFSObject())
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }
}