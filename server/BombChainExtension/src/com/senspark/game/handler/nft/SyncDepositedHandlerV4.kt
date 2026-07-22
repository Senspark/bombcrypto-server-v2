package com.senspark.game.handler.nft

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.exception.CustomException
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class SyncDepositedHandlerV4 : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.SYNC_DEPOSITED_V4

    // Client routes which deposit sync to run. Phase 9: the cross-chain bridge no longer credits an in-game
    // balance (bridge balance is read on-chain), so the BRIDGE branch is a no-op here — only the OLD in-game
    // deposit sync remains. The field is kept for client compatibility; absent → BOTH.
    companion object {
        private const val TARGET_OLD = "OLD"
        private const val TARGET_BOTH = "BOTH"
    }

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val target = if (data.containsKey(SFSField.DEPOSIT_SYNC_TARGET))
                data.getUtfString(SFSField.DEPOSIT_SYNC_TARGET) else TARGET_BOTH
            if (target == TARGET_OLD || target == TARGET_BOTH) {
                controller.masterUserManager.userDepositManager.syncDepositedV4()
            }
            val result: ISFSObject = SFSObject()
            result.putSFSArray(SFSField.Rewards, controller.masterUserManager.blockRewardManager.toSfsArrays())
            return sendSuccess(controller, requestId, result)
        } catch (ex: CustomException) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}
