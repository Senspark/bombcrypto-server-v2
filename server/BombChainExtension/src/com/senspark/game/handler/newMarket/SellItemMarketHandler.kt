package com.senspark.game.handler.newMarket

import com.senspark.game.api.SellOrEditDataRequest
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class SellItemMarketHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.SELL_ITEM_MARKET_V3

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            
            val sellData = SellOrEditDataRequest(
                sellerUid = controller.userId,
                itemId = data.getInt("item_id"),
                quantity = data.getInt("quantity"),
                price = data.getDouble("price"),
                oldPrice = data.getDouble("price"),
                itemType = data.getInt("item_type"),
                listId = emptyList(), // update later
                expiration = data.getInt("expiration"),
                modifyDate = System.currentTimeMillis() / 1000,
                oldQuantity = 0 // not used in sell
            )
            
            val isSuccess  = controller.masterUserManager.userMarketplaceManager.sellV3(sellData)

            if (!isSuccess) {
                throw Exception("User ${controller.userId} Sell item failed")
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