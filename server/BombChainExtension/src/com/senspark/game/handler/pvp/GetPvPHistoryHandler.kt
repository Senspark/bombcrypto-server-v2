package com.senspark.game.handler.pvp

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class GetPvPHistoryHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_PVP_HISTORY_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        val response = SFSObject()
        try {
            response.putSFSArray("history", controller.getPvPHistory(data.getInt("at"), data.getInt("count")))
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
        return sendSuccess(controller, requestId, response)
    }
}