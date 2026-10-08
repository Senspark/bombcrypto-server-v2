package com.senspark.game.handler.convertToken

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand.PREVIEW_TOKEN_V2
import com.senspark.game.exception.CustomException
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class PreviewTokenHandler : BaseEncryptRequestHandler() {
    override val serverCommand = PREVIEW_TOKEN_V2

    // Closed alongside SWAP_TOKEN_V2 -- quoting a swap that cannot be executed is worse than refusing it.
    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        sendExceptionError(
            controller,
            requestId,
            CustomException("Gem swap is no longer available", ErrorCode.NOT_SUPPORTED)
        )
    }
}
