package com.senspark.game.handler.rock

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.exception.CustomException
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.rock.IUserRockManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class CreateRockHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.CREATE_ROCK_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            if (controller.userInfo.type != EnumConstants.UserType.FI) {
                throw CustomException("You aren't user FI")
            }
            val userRockManager = controller.svServices.get<IUserRockManager>()
            val tx = data.getUtfString("tx")
            val listIdHero = data.getIntArray("listIdHero").toList()
            val totalRockReceived = userRockManager.createRock(controller, tx, controller.walletAddress, listIdHero)
            val response: ISFSObject = SFSObject()
            if (totalRockReceived == 0f) {
                response.putInt("code", 100)
                return sendSuccess(controller, requestId, response)
            }
            response.putInt("code", 0)
            response.putSFSArray(SFSField.Rewards, controller.masterUserManager.blockRewardManager.toSfsArrays())
            return sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}