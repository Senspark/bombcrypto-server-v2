package com.senspark.game.handler.pvp

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.pvp.IPvpResultManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class ClaimPvpMatchRewardHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.CLAIM_PVP_MATCH_REWARD_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        val response = SFSObject()
        try {
            val resultManager = controller.svServices.get<IPvpResultManager>()
            val reward = resultManager.claimReward(controller.userId) ?: throw Exception("Reward not found")
            response.apply {
                putUtfString("reward_id", reward.rewardId)
                putBool("is_out_of_chest_slot", reward.isOutOfChestSlot)
            }
            return sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            return sendError(controller, requestId, 100, ex.message)
        }
    }
}