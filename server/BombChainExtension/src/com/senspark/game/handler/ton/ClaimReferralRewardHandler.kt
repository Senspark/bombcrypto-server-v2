package com.senspark.game.handler.ton

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.ton.IReferralManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class ClaimReferralRewardHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.CLAIM_REFERRAL_REWARD_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (controller.dataType != EnumConstants.DataType.TON) {
            return sendError(controller, requestId, ErrorCode.NOT_USER_TON, null)
        }

        try {
            controller.svServices.get<IReferralManager>().claimRewards(controller)
            controller.saveGameAndLoadReward()
            val response = SFSObject().apply {
                putSFSArray(SFSField.Rewards, controller.masterUserManager.blockRewardManager.toSfsArrays())
            }
            return sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            return sendExceptionError(controller, requestId, e)
        }
    }
}