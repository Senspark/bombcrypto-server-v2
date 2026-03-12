package com.senspark.game.handler.dailyTask

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class ClaimDailyTaskHandler() : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.CLAIM_DAILY_TASK

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            val response = SFSObject()
            val isClaimSuccess = if (data.containsKey("task_id")) {
                handleTaskRewardClaim(controller, data)
            } else {
                handleFinalRewardClaim(controller, response)
            }
            if (isClaimSuccess) {
                sendSuccess(controller, requestId, response)
            } else {
                sendError(controller, requestId, ErrorCode.CLAIM_DAILY_TASK_FAIL, null)
            }

        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }

    private fun handleFinalRewardClaim(controller: IUserController, response: SFSObject): Boolean {
        val finalReward = controller.masterUserManager.userDailyTaskManager.claimFinalReward()
        finalReward.forEach {
            controller.masterUserManager.userInventoryManager.addItem(
                it.itemId,
                it.quantity,
                it.expirationAfter,
                "Claim final daily reward"
            )
        }
        val sfsArr = SFSArray()
        finalReward.forEach {
            val obj = SFSObject()
            obj.putInt("item_id", it.itemId)
            obj.putInt("quantity", it.quantity)
            sfsArr.addSFSObject(obj)
        }
        response.putSFSArray("final_reward", sfsArr)

        val blockRewardManager = controller.masterUserManager.blockRewardManager
        blockRewardManager.loadUserBlockReward()
        response.putSFSArray("rewards", blockRewardManager.toSfsArrays())
        
        return finalReward.isNotEmpty()
    }

    private fun handleTaskRewardClaim(
        controller: IUserController,
        data: ISFSObject,
    ): Boolean {
        val taskId = data.getInt("task_id")
        val result = controller.masterUserManager.userDailyTaskManager.claimTaskReward(taskId)
        if (result != null) {
            val invManager = controller.masterUserManager.userInventoryManager
            result.forEach {
                invManager.addItem(it.itemId, it.quantity, it.expiration, "Claim daily reward")
            }
        } else {
            return false
        }
        return result.isNotEmpty()
    }
}