package com.senspark.game.handler.request

import com.senspark.game.controller.IUserController
import com.senspark.game.controller.LegacyUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.exception.CustomException
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.claim.IClaimResponseManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class ApproveClaimHandlerV4 : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.APPROVE_CLAIM_V4

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        controller as LegacyUserController
        if (controller.disableWhileLoginByAccount()) {
            return sendError(controller, requestId, ErrorCode.PERMISSION_DENIED, null)
        }
        try {
            val walletAddress = controller.walletAddress
            if (controller.userInfo.type != EnumConstants.UserType.FI || walletAddress.isNullOrEmpty()) {
                throw CustomException("You aren't user FI")
            }
            val blockRewardType = EnumConstants.BLOCK_REWARD_TYPE.valueOf(data.getInt(SFSField.BLOCK_REWARD_TYPE))
            val prep = controller.masterUserManager.claimManager.prepareClaimV4(blockRewardType)

            val correlationId = controller.svServices.get<IClaimResponseManager>()
                .beginCheck(controller, walletAddress, blockRewardType, prep.tokenTypeInBlockChain)

            val responseData = SFSObject()
            responseData.putUtfString("status", "PENDING")
            responseData.putUtfString("correlationId", correlationId)
            sendSuccess(controller, requestId, responseData)
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}
