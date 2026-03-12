package com.senspark.game.handler.stake

import com.senspark.game.controller.IUserController
import com.senspark.game.controller.LegacyUserController
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.KickReason
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class UserStakeHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.USER_STAKE_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (!controller.checkHash()) {
            controller.disconnect(KickReason.CHEAT_LOGIN)
            return
        }
        controller as LegacyUserController
        return try {
            val blockRewardType = data.getUtfString(SFSField.BLOCK_REWARD_TYPE)
            val amount = data.getFloat(SFSField.AMOUNT).toFloat()
            val allIn = data.getBool(SFSField.ALL_IN)
            val result = controller.userStake(BLOCK_REWARD_TYPE.valueOf(blockRewardType), amount, allIn)
            result.putSFSArray(SFSField.Rewards, controller.masterUserManager.blockRewardManager.toSfsArrays())
            sendSuccess(controller, requestId, result)
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}