package com.senspark.game.handler.pvp

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants.DeviceType
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class GetBonusRewardPvpHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_BONUS_REWARD_PVP_V3

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        coroutine.scope.launch(Dispatchers.Default) {
            try {
                val rewardId = data.getUtfString("reward_id")
                val adsId = data.getUtfString("ads_id")
                val deviceType = DeviceType.valueOf(data.getUtfString("device_type"))
                val response = controller.masterUserManager.userBonusRewardManager.takeLuckyWheelReward(
                    rewardId,
                    adsId,
                    deviceType
                )
                sendSuccess(controller, requestId, response)
            } catch (ex: Exception) {
                sendExceptionError(controller, requestId, ex)
            }
        }
    }
}