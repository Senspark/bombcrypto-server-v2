package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class GetRewardV2Handler : BaseEncryptRequestHandler() {
    override val serverCommand: String = SFSCommand.GET_REWARD_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        scheduler.fireAndForget {
            try {
                synchronized(controller.locker) {
                    controller.saveGameAndLoadReward()
                    val result = SFSObject()

                    val rewards = controller.masterUserManager.blockRewardManager.toSfsArrays()
                    result.putSFSArray(SFSField.Rewards, rewards)
                    sendSuccess(controller, requestId, result)
                }
            } catch (ex: Exception) {
                sendExceptionError(controller, requestId, ex)
            }
        }
    }
}