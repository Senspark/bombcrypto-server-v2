package com.senspark.game.handler.misc

import com.senspark.common.utils.toSFSArray
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.extension.coroutines.ICoroutineScope
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.offlineReward.IOfflineRewardManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ClaimOfflineRewardsHandler : BaseEncryptRequestHandler() {
    override val serverCommand: String = SFSCommand.CLAIM_OFFLINE_REWARDS_V2
    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        coroutine.scope.launch(Dispatchers.Default) {
            try {
                val offlineRewardManager = controller.svServices.get<IOfflineRewardManager>()
                val adsToken = if (data.containsKey("ads_token")) data.getUtfString("ads_token") else null
                val pair = if (adsToken != null)
                    offlineRewardManager.claimRewardsWithAds(controller, adsToken)
                else
                    offlineRewardManager.claimRewards(controller)
                val response = SFSObject().apply {
                    putInt("offline_hours", pair.first)
                    putSFSArray("items", pair.second.toSFSArray {
                        SFSObject().apply {
                            putInt("item_id", it.key)
                            putInt("quantity", it.value)
                        }
                    })
                }
                sendSuccess(controller, requestId, response)
            } catch (e: Exception) {
                controller.logger.error("ClaimOfflineRewardsHandler", e)
                sendExceptionError(controller, requestId, e)
            }
        }

    }
}