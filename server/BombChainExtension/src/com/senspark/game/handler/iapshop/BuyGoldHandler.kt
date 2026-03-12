package com.senspark.game.handler.iapshop

import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.iap.IIAPShopManager
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class BuyGoldHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.BUY_GOLD_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val iapShopManager = controller.svServices.get<IIAPShopManager>()
            val itemId = data.getInt("item_id")
            iapShopManager.buyGold(controller, itemId)
            val response = SFSObject.newInstance().apply { putBool("success", true) }
            sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }
}