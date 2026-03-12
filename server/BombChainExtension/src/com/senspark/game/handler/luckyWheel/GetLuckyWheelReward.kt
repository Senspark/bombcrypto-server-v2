package com.senspark.game.handler.luckyWheel

import com.senspark.common.utils.toSFSArray
import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.luckyWheel.ILuckyWheelRewardManager
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class GetLuckyWheelReward : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_LUCKY_WHEEL_REWARD_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val luckyWheelRewardManager = controller.svServices.get<ILuckyWheelRewardManager>()
            val result = luckyWheelRewardManager.rewards.toSFSArray { it.toSfsObject() }
            val response = SFSObject().apply { putSFSArray("data", result) }
            sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}