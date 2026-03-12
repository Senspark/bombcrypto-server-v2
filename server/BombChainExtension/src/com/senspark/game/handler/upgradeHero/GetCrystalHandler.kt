package com.senspark.game.handler.upgradeHero

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class GetCrystalHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_CRYSTALS_V2
    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            val manager = controller.masterUserManager.userMaterialManager
            val response = SFSObject().apply { putSFSArray("data", manager.toSfsArray()) }
            sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}