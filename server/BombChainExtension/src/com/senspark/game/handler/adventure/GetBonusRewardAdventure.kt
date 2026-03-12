package com.senspark.game.handler.adventure

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.SFSCommand
import com.senspark.game.extension.coroutines.ICoroutineScope
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class GetBonusRewardAdventureHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_BONUS_REWARD_ADVENTURE_V3

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        coroutine.scope.launch(Dispatchers.Default) {
            try {
                val manager = controller.masterUserManager.userAdventureModeManager
                val rewardId = data.getUtfString("reward_id")
                val adsId = data.getUtfString("ads_id")
                val deviceType = EnumConstants.DeviceType.valueOf(data.getUtfString("device_type"))
                val response = manager.takeLuckyWheelReward(rewardId, adsId, deviceType)
                sendSuccess(controller, requestId, response)
            } catch (ex: Exception) {
                sendExceptionError(controller, requestId, ex)
            }
        }

    }
}