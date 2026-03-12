package com.senspark.game.handler.data

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class CheckNoAdsHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.CHECK_NO_ADS_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val noAds = controller.masterUserManager.userConfigManager.noAds
            sendSuccess(controller, requestId, SFSObject().apply {
                putBool("no_ads", noAds)
            })
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}