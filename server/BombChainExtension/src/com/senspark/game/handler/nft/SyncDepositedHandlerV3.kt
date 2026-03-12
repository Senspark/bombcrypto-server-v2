package com.senspark.game.handler.nft

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.exception.CustomException
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class SyncDepositedHandlerV3 : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.SYNC_DEPOSITED_V3

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            controller.masterUserManager.userDataManager.syncDepositedV3()
            val result: ISFSObject = SFSObject()
            result.putSFSArray(SFSField.Rewards, controller.masterUserManager.blockRewardManager.toSfsArrays())
            return sendSuccess(controller, requestId, result)
        } catch (ex: CustomException) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}

