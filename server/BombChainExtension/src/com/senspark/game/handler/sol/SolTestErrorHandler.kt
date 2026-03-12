package com.senspark.game.handler.sol

import com.senspark.game.controller.AirdropUserController
import com.senspark.game.controller.IUserController
import com.smartfoxserver.v2.entities.data.ISFSObject

class SolTestErrorHandler : BaseEncryptRequestHandler() {
    override val serverCommand: String = "SOL_TEST_ERROR"
    
    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        sendError(controller, requestId, 1, "This command will always fail")
    }
}