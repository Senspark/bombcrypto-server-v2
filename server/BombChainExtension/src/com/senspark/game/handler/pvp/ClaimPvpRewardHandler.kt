package com.senspark.game.handler.pvp

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.KickReason
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class ClaimPvpRewardHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.CLAIM_MONTHLY_REWARD_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            if (controller.checkHash()) {
                controller.disconnect(KickReason.CHEAT_LOGIN)
            }
            controller.masterUserManager.blockRewardManager.loadUserBlockReward()
            val response = controller.masterUserManager.userPvpRankingManager.claimReward()
            sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}