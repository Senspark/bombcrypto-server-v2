package com.senspark.game.handler.shield

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.nftShield.INFTShieldManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class VerifyNftShieldPinHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.VERIFY_NFT_SHIELD_PIN

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            val shieldManager = controller.svServices.get<INFTShieldManager>()
            val pin = data.getUtfString("pin")
            require(pin != null) { "PIN cannot be null" }

            val isValid = shieldManager.verifyPin(controller.userId, pin)
            val isLocked = shieldManager.isLocked(controller.userId)
            val failedAttempts = shieldManager.getFailedAttempts(controller.userId)
            val lockUntil = shieldManager.getLockUntil(controller.userId)

            val response: ISFSObject = SFSObject()
            response.putBool("is_valid", isValid)
            response.putBool("is_locked", isLocked)
            response.putInt("failed_attempts", failedAttempts)
            if (lockUntil != null) {
                response.putLong("lock_until", lockUntil)
            }

            return sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}
