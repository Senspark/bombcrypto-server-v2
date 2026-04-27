package com.senspark.game.handler.shield

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.nftShield.INFTShieldManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class GenerateNftShieldSignatureHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GENERATE_NFT_SHIELD_SIGNATURE

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            val shieldManager = controller.svServices.get<INFTShieldManager>()
            
            val walletAddress = data.getUtfString("wallet_address")
            val nonce = data.getLong("nonce")
            val tokenIdsArray = data.getLongArray("token_ids")
            val pin = data.getUtfString("pin")
            
            require(walletAddress != null) { "Wallet address required" }
            require(nonce != null) { "Nonce required" }
            require(tokenIdsArray != null && tokenIdsArray.isNotEmpty()) { "Token IDs required" }
            require(pin != null) { "PIN required" }

            val isValid = shieldManager.verifyPin(controller.userId, pin)
            require(isValid) { "Invalid PIN or Shield Locked" }

            val signature = shieldManager.generateSignature(walletAddress, nonce, tokenIdsArray.toList())

            val response: ISFSObject = SFSObject()
            response.putUtfString("signature", signature)

            return sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}
