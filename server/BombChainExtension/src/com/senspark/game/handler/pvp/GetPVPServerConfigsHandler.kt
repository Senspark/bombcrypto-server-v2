package com.senspark.game.handler.pvp

import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.pvp.IPvpConfigManager
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.utils.serialize
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class GetPVPServerConfigsHandler : BaseEncryptRequestHandler() {
    override val serverCommand: String = SFSCommand.GET_PVP_SERVER_CONFIGS_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            val pvpConfigManager = controller.svServices.get<IPvpConfigManager>()
            val response = SFSObject.newFromJsonData(pvpConfigManager.getConfig().serialize())
            return sendSuccess(controller, requestId, response)
        }
        catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}