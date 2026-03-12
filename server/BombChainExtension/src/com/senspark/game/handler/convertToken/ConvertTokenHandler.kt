package com.senspark.game.handler.convertToken

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.convertToken.ISwapTokenRealtimeManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class ConvertTokenHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.SWAP_TOKEN_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            val response = SFSObject()
            val balance = data.getFloat("balance")
            val networkType = data.getInt("network_type")
            val tokenType = data.getInt("token_type")
            val swapToken = controller.svServices.get<ISwapTokenRealtimeManager>()
            val result = swapToken.tokenConvert(
                controller.userId,
                controller,
                balance,
                ISwapTokenRealtimeManager.NetworkType.from(networkType),
                tokenType
            )
            controller.logger.log("[ConvertTokenHandler:handleExtensionRequest] result: $result")

            response.putFloat("data", result)
            return sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            return sendExceptionError(controller, requestId, ex)
        }
    }
}