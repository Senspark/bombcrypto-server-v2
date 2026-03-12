package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class UserRentHouseHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.RENT_HOUSE
    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (!controller.isAirdropUser()) {
            return sendError(controller, requestId, ErrorCode.NOT_USER_AIRDROP, null)
        }
        return try {
            val houseId = data.getInt("house_id")
            val numDays = data.getInt("num_days")
            val endTimeRent = controller.masterUserManager.houseManager.rentHouse(houseId, numDays)
            controller.masterUserManager.blockRewardManager.loadUserBlockReward()

            val response = SFSObject().apply {
                putLong("end_time_rent", endTimeRent)
                putSFSArray(SFSField.Rewards, controller.masterUserManager.blockRewardManager.toSfsArrays())
            }
            sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }
}