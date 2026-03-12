package com.senspark.game.handler.stake

import com.senspark.game.controller.IUserController
import com.senspark.game.controller.LegacyUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.exception.CustomException
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class UserWithdrawStakeHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.USER_WITHDRAW_STAKE_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        controller as LegacyUserController
        return try {
            val isWithdraw = data.getBool(SFSField.IS_WITHDRAW)
            val result = controller.userWithdrawStake(isWithdraw)
            result.putSFSArray(SFSField.Rewards, controller.masterUserManager.blockRewardManager.toSfsArrays())
            sendSuccess(controller, requestId, result)
        } catch (ex: CustomException) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}