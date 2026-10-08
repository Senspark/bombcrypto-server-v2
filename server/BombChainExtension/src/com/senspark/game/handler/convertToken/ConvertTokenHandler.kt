package com.senspark.game.handler.convertToken

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.exception.CustomException
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class ConvertTokenHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.SWAP_TOKEN_V2

    // Gem swap is closed. The command stays registered so an old client gets an error it can show
    // instead of a request that never comes back. sendExceptionError rather than throw:
    // BaseEncryptRequestHandler catches whatever escapes this method and logs it as a decrypt failure
    // without replying to the client.
    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        sendExceptionError(
            controller,
            requestId,
            CustomException("Gem swap is no longer available", ErrorCode.NOT_SUPPORTED)
        )
    }
}
