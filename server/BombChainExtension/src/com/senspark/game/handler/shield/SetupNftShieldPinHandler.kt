package com.senspark.game.handler.shield

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.nftShield.INFTShieldManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class SetupNftShieldPinHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.SETUP_NFT_SHIELD_PIN

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            val shieldManager = controller.svServices.get<INFTShieldManager>()
            val pin = data.getUtfString("pin")
            require(pin != null && pin.length == 4 && pin.all { it.isDigit() }) { "Invalid PIN format" }

            shieldManager.setupPin(controller.userId, pin)

            val response: ISFSObject = SFSObject()
            return sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}
