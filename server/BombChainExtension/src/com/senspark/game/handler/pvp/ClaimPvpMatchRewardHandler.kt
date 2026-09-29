package com.senspark.game.handler.pvp

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.pvp.IPvpResultManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ClaimPvpMatchRewardHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.CLAIM_PVP_MATCH_REWARD_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        // The match result may still be on its way from the pvp server: wait for it, then respond.
        coroutine.scope.launch(Dispatchers.Default) {
            val response = SFSObject()
            try {
                val resultManager = controller.svServices.get<IPvpResultManager>()
                val reward = resultManager.claimReward(controller.userId) ?: throw Exception("Reward not found")
                response.apply {
                    putUtfString("reward_id", reward.rewardId)
                    putBool("is_out_of_chest_slot", reward.isOutOfChestSlot)
                    putBool(SFSField.HAS_HERO_CAGE, reward.hasHeroCage)
                }
                sendSuccess(controller, requestId, response)
            } catch (ex: Exception) {
                sendError(controller, requestId, 100, ex.message)
            }
        }
    }
}
