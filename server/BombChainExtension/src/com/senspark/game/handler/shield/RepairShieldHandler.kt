package com.senspark.game.handler.shield

import com.senspark.game.controller.IUserController
import com.senspark.game.controller.LegacyUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class RepairShieldHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.REPAIR_SHIELD_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        controller as LegacyUserController
        if (controller.disableWhileLoginByAccount()) {
            return sendError(controller, requestId, ErrorCode.PERMISSION_DENIED, null)
        }
        return try {
            val heroId = data.getInt("hero_id")
            val rewardType = EnumConstants.BLOCK_REWARD_TYPE.valueOf(data.getInt("reward_type"))
            val repairedHero = controller.masterUserManager.heroFiManager.repairShield(rewardType, heroId)

            //Load lại rock
            val blockRewardManager = controller.masterUserManager.blockRewardManager
            blockRewardManager.loadUserBlockReward()

            val dataResponse = SFSObject()
            dataResponse.putSFSObject("repaired_hero", repairedHero)
            dataResponse.putSFSArray("rewards", blockRewardManager.toSfsArrays())
            return sendSuccess(controller, requestId, dataResponse)
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }
}