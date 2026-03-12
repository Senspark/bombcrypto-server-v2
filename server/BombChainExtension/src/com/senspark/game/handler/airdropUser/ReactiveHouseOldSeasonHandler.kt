package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class ReactiveHouseOldSeasonHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.REACTIVE_HOUSE_OLD_SEASON

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (!controller.isAirdropUser()) {
            return sendError(controller, requestId, ErrorCode.NOT_USER_AIRDROP, null)
        }
        try {
            val houseId = data.getInt("house_id")
            controller.masterUserManager.houseManager.reactiveHouseOldSeason(houseId)
            controller.masterUserManager.blockRewardManager.loadUserBlockReward()
            val response = SFSObject().apply { 
                putSFSArray(SFSField.Rewards, controller.masterUserManager.blockRewardManager.toSfsArrays())
            }
            return sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            return sendExceptionError(controller, requestId, e)
        }
    }
}