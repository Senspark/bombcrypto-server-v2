package com.senspark.game.handler.iapshop

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.customEnum.IAPShopType
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class UserBuyGemHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.BUY_GEM_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val packageName = data.getUtfString("package_name")
            val productId = data.getUtfString("product_id")
            val billToken = data.getUtfString("bill_token")
            val storeId = data.getInt("store_id")
            val transactionId = data.getUtfString("transaction_id")
            val isSpecialOffer =
                if (data.containsKey("is_special_offer")) data.getBool("is_special_offer") else false
            controller.masterUserManager.userIAPShopManager.buy(
                IAPShopType.GEM,
                packageName,
                productId,
                billToken,
                transactionId,
                storeId,
                isSpecialOffer
            )
            val response = SFSObject.newInstance().apply { putBool("success", true) }
            return sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }
}