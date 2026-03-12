package com.senspark.game.handler.pvp

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class GetUserPvpBoosterHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_USER_PVP_BOOSTERS_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            val sfsObject = SFSObject()
            controller.masterUserManager.userPvPBoosterManager.loadFromDb()
            sfsObject.putSFSArray("boosters", controller.masterUserManager.userPvPBoosterManager.toSfsArray())
            return sendSuccess(controller, requestId, sfsObject)
        }
        catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}