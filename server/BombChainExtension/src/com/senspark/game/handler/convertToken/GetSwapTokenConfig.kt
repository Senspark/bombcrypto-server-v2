package com.senspark.game.handler.convertToken

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.convertToken.ISwapTokenRealtimeManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class GetSwapTokenConfig : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_SWAP_TOKEN_CONFIG_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            val swapToken = controller.svServices.get<ISwapTokenRealtimeManager>()
            val response: ISFSObject = SFSObject()
            response.putInt("min_gem_swap", swapToken.getMinGemSwap())

            return sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}