package com.senspark.game.handler.pvp

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.user.ITrGameplayManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class PvpLeaveQueueHandler : BaseEncryptRequestHandler() {

    override val serverCommand = SFSCommand.LEAVE_PVP_QUEUE_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        val trGameplayManager = services.get<ITrGameplayManager>()
        trGameplayManager.leavePvp(controller.userId, controller.dataType)

        val response = SFSObject()
        val result = controller.leavePvPQueue()
        if (!result) {
            return sendError(controller, requestId, 100, "Could not leave pvp queue")
        }
        return sendSuccess(controller, requestId, response)
    }
}