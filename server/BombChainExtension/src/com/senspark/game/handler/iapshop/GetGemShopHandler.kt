package com.senspark.game.handler.iapshop

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class GetGemShopHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_GEM_SHOP_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            val iapShopManager = controller.masterUserManager.userIAPShopManager
            val response = SFSObject().apply {
                putSFSArray("data", iapShopManager.getGemShop())
            }
            return sendSuccess(controller, requestId, response)
        }
        catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}