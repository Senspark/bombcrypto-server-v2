package com.senspark.game.handler.dailyMission

import com.senspark.common.utils.toSFSArray
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class TakeMissionRewardHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.TAKE_DAILY_MISSION_REWARD_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val manager = controller.masterUserManager.userMissionManager
            val missionCode = data.getUtfString("mission_code")
            val result = manager.takeReward(missionCode)
            val response = SFSObject().apply { putSFSArray("data", result.toSFSArray { it.toSfsObject() }) }
            sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}