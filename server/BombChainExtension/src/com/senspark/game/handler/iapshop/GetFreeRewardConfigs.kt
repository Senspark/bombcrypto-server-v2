package com.senspark.game.handler.iapshop

import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.iap.IIAPShopManager
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.utils.serialize
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class GetFreeRewardConfigsHandler : BaseEncryptRequestHandler() {
    override val serverCommand: String = SFSCommand.GET_FREE_REWARD_CONFIGS_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val iapShopManager = controller.svServices.get<IIAPShopManager>()
            val configs = iapShopManager.getUserFreeRewardConfigs(controller)
            val response = SFSObject().apply {
                putSFSArray("data", SFSArray.newFromJsonData(configs.serialize()))
            }
            sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            controller.logger.error("Error when get free reward configs", e)
            sendExceptionError(controller, requestId, e)
        }
    }
}