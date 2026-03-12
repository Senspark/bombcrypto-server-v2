package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class UserStartAutoMineV2Handler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.START_AUTO_MINE_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val result = controller.masterUserManager.userAutoMineManager.startAutoMine()
            sendSuccess(controller, requestId, result)
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }
}