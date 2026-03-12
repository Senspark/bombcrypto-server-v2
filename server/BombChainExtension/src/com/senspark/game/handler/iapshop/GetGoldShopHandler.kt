package com.senspark.game.handler.iapshop

import com.senspark.common.utils.toSFSArray
import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.iap.IIAPShopManager
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class GetGoldShopHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_GOLD_SHOP_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            val iapShopManager = controller.svServices.get<IIAPShopManager>()
            val configs = iapShopManager.getGoldShopConfigs()
            val response = SFSObject().apply {
                putSFSArray("data", configs.toSFSArray { it.toSFSObject() })
            }
            return sendSuccess(controller, requestId, response)
        }
        catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}