package com.senspark.game.handler.ton

import com.senspark.game.controller.AirdropUserController
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class GetOfflineRewardTHModeHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_OFFLINE_TH_MODE_REWARD_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        controller as AirdropUserController
        if (controller.dataType != EnumConstants.DataType.TON) {
            return sendError(controller, requestId, ErrorCode.NOT_USER_TON, null)
        }
        if (controller.isReceivedOfflineTHMode) {
            return sendSuccess(controller, requestId, SFSObject())
        }
        val activeHeroes = controller.masterUserManager.heroFiManager.activeHeroes
        val activeHouse = controller.masterUserManager.houseManager.activeHouse

        val response = controller.masterUserManager.userAutoMineManager.getOfflineReward(activeHeroes, activeHouse)
        controller.masterUserManager.blockRewardManager.loadUserBlockReward()
        response.putSFSArray(SFSField.Rewards, controller.masterUserManager.blockRewardManager.toSfsArrays())

        controller.isReceivedOfflineTHMode = true
        return sendSuccess(controller, requestId, response)
    }
}
