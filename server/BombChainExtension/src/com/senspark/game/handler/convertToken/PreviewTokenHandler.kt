package com.senspark.game.handler.convertToken

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand.PREVIEW_TOKEN_V2
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.convertToken.ISwapTokenRealtimeManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class PreviewTokenHandler : BaseEncryptRequestHandler() {
    override val serverCommand = PREVIEW_TOKEN_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        val balance = data.getFloat("balance")
        val networkType = data.getInt("network_type")
        val tokenType = data.getInt("token_type")
        val swapToken = controller.svServices.get<ISwapTokenRealtimeManager>()
        val result = swapToken.previewConversion(
            balance,
            ISwapTokenRealtimeManager.NetworkType.from(networkType),
            tokenType
        )
        controller.logger.log("[PreviewTokenHandler:handleExtensionRequest] result: $result")

        val response = SFSObject()
        response.putInt(
            "code", try {
                response.putFloat(
                    "data",
                    result
                )
                0
            } catch (ex: Exception) {
                response.putUtfString("message", ex.message ?: "")
                100
            }
        )
        return sendSuccess(controller, requestId, response)
    }
}