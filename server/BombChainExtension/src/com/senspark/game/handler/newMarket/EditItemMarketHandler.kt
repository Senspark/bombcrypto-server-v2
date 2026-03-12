package com.senspark.game.handler.newMarket

import com.senspark.game.api.SellOrEditDataRequest
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class EditItemMarketHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.EDIT_ITEM_MARKET_V3

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {

            val sellData = SellOrEditDataRequest(
                sellerUid = controller.userId,
                itemId = data.getInt("item_id"),
                quantity = data.getInt("new_quantity"),
                oldQuantity = data.getInt("old_quantity"),
                price = data.getDouble("new_price"),
                oldPrice = data.getDouble("old_price"),
                itemType = data.getInt("item_type"),
                listId = emptyList(), // update later
                expiration = data.getInt("expiration"),
                modifyDate = System.currentTimeMillis() / 1000
            )

            val isSuccess  = controller.masterUserManager.userMarketplaceManager.editV3(sellData)

            if(!isSuccess) {
                throw Exception("Edit item market failed")
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