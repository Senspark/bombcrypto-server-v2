package com.senspark.game.handler.onBoarding

import com.senspark.common.utils.toSFSArray
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.onBoarding.UserProgress
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class UpdateUserOnBoardingHandler() : BaseEncryptRequestHandler(
) {
    override val serverCommand = SFSCommand.UPDATE_USER_ON_BOARDING
    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (controller.isAirdropUser()) {
            return sendError(controller, requestId, ErrorCode.NOT_SUPPORT_AIRDROP_USER, null)
        }
        try {
            val newStep = data.getInt("step") ?: 0
            val newClaimed = data.getInt("claimed") ?: 0

            val userProgress = UserProgress(
                userId = controller.userId,
                newStep = newStep,
                newClaimed = newClaimed,
                network = controller.dataType.getCoinType(true).name,
                rewardType = EnumConstants.BLOCK_REWARD_TYPE.COIN.name,
                
                // Sẽ đc update sau
                currentClaimed = 0,
                currentStep = 0
            )
            
            controller.masterUserManager.userOnBoardingManager.updateUserProgress(userProgress)
     
            val response = SFSObject()
            val blockRewardManager = controller.masterUserManager.blockRewardManager
            blockRewardManager.loadUserBlockReward()

            response.putSFSArray("rewards", blockRewardManager.toSfsArrays())
            return sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            return sendExceptionError(controller, requestId, e)
        }
    }
}