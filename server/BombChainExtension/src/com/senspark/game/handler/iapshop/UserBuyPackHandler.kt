package com.senspark.game.handler.iapshop

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.customEnum.IAPShopType
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class UserBuyPackHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.BUY_PACK_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val packageName = data.getUtfString("package_name")
            val productId = data.getUtfString("product_id")
            val billToken = data.getUtfString("bill_token")
            val storeId = data.getInt("store_id")
            val transactionId = data.getUtfString("transaction_id")
            controller.masterUserManager.userIAPShopManager.buy(
                IAPShopType.PACK,
                packageName,
                productId,
                billToken,
                transactionId,
                storeId,
            )
            val response = SFSObject.newInstance().apply { putBool("success", true) }
            return sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }
}