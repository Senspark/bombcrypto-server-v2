package com.senspark.game.handler.sol

import com.senspark.game.controller.IUserController
import com.senspark.game.utils.serialize
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.Serializable

class SolTestSuccessHandler : BaseEncryptRequestHandler() {
    override val serverCommand: String = "SOL_TEST_SUCCESS"

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        val response = Response("Hello from Server")
        sendSuccess(controller, requestId, SFSObject().apply { putUtfString("data", response.serialize()) })
    }

    @Serializable
    private data class Response(val message: String)
}

