package com.senspark.game.handler.dailyMission

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WatchingDailyAdsHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.WATCHING_DAILY_MISSION_ADS_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        coroutine.scope.launch(Dispatchers.Default) {
            try {
                val manager = controller.masterUserManager.userMissionManager
                val missionCode = data.getUtfString("mission_code")
                val adsToken = data.getUtfString("ads_token")
                manager.watchAds(missionCode, adsToken)
                sendSuccess(controller, requestId, SFSObject())
            } catch (ex: Exception) {
                sendExceptionError(controller, requestId, ex)
            }
        }
    }
}