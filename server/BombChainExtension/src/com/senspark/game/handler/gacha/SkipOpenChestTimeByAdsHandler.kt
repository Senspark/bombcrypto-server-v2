package com.senspark.game.handler.gacha

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SkipOpenChestTimeByAdsHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.SKIP_OPEN_CHEST_TIME_BY_ADS_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        coroutine.scope.launch(Dispatchers.Default) {
            try {
                val chestId = data.getInt("chest_id")
                val token = data.getUtfString("token")
                val manager = controller.masterUserManager.userGachaChestManager
                val remainingTime = manager.skipOpenTimeByAds(chestId, token)
                val responseData = SFSObject.newInstance().apply { putLong("remaining_time", remainingTime) }
                sendSuccess(controller, requestId, responseData)
            } catch (e: Exception) {
                sendExceptionError(controller, requestId, e)
            }
        }
    }
}