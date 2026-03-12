package com.senspark.game.handler.misc

import com.senspark.game.api.IServerInfoManager
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class GetServerInfoHandler : BaseEncryptRequestHandler() {
    override val serverCommand: String = SFSCommand.GET_SERVER_INFO_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            val serverInfoManager = controller.svServices.get<IServerInfoManager>()
            if (!serverInfoManager.isEnable()) {
                return sendError(controller, requestId, 100, "Server info is not enable")
            }
            val response = serverInfoManager.getServerInfo()
            return sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            controller.logger.error("Get server info error", e)
            sendExceptionError(controller, requestId, e)
        }
    }
}