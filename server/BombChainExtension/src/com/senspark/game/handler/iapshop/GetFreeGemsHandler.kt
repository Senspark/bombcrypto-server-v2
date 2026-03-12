package com.senspark.game.handler.iapshop

import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.iap.IIAPShopManager
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GetFreeGemsHandler : BaseEncryptRequestHandler() {
    override val serverCommand: String = SFSCommand.GET_FREE_GEMS_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        coroutine.scope.launch(Dispatchers.Default) {
            try {
                val iapShopManager = controller.svServices.get<IIAPShopManager>()
                val token = data.getUtfString("token")
                iapShopManager.getFreeRewardByAds(controller, token, EnumConstants.BLOCK_REWARD_TYPE.GEM_LOCKED)
                sendSuccess(controller, requestId, SFSObject())
            } catch (e: Exception) {
                controller.logger.error("Error when get free gem", e)
                sendExceptionError(controller, requestId, e)
            }
        }
        
    }
}