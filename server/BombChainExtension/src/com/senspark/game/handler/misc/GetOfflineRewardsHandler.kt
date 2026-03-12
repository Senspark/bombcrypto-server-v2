package com.senspark.game.handler.misc

import com.senspark.common.utils.toSFSArray
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.offlineReward.IOfflineRewardManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class GetOfflineRewardsHandler : BaseEncryptRequestHandler() {
    override val serverCommand: String = SFSCommand.GET_OFFLINE_REWARDS_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            val offlineRewardManager = controller.svServices.get<IOfflineRewardManager>()
            val lastLogout = controller.userInfo.lastLogout
            val rewards = offlineRewardManager.getRewards(controller)
            val response = SFSObject.newInstance().apply {
                putSFSArray("items", rewards.second.toSFSArray {
                    SFSObject().apply {
                        putInt("item_id", it.key)
                        putInt("quantity", it.value)
                    }
                })
                putLong("last_logout", lastLogout?.toEpochMilli() ?: 0)
            }
            return sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            controller.logger.error("GetOfflineRewardsHandler", e)
            return sendExceptionError(controller, requestId, e)
        }
    }
}