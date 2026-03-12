package com.senspark.game.handler.request

import com.senspark.game.controller.IUserController
import com.senspark.game.controller.LegacyUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand.APPROVE_CLAIM_V2
import com.senspark.game.declare.SFSCommand.CONFIRM_CLAIM_REWARD_SUCCESS_V2
import com.senspark.game.declare.SFSField
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class ApproveClaimWithoutConfirmHandler : ApproveClaimHandler(APPROVE_CLAIM_V2, false)
class ConfirmClaimHandler : ApproveClaimHandler(CONFIRM_CLAIM_REWARD_SUCCESS_V2, true)

open class ApproveClaimHandler(
    val cmd: String,
    private val isConfirmClaimSuccess: Boolean
) : BaseEncryptRequestHandler() {
    override val serverCommand = cmd

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        controller as LegacyUserController
        if (controller.disableWhileLoginByAccount()) {
            return sendError(controller, requestId, ErrorCode.PERMISSION_DENIED, null)
        }
        return try {
            val blockRewardType = EnumConstants.BLOCK_REWARD_TYPE.valueOf(data.getInt(SFSField.BLOCK_REWARD_TYPE))
            val responseData = if (!isConfirmClaimSuccess) {
                controller.masterUserManager.claimManager.claimReward(blockRewardType)
            } else {
                controller.masterUserManager.claimManager.confirmClaimSuccess(blockRewardType)
            }
            sendSuccess(controller, requestId, responseData)
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}