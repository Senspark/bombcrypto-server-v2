package com.senspark.game.handler.pvp

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.pvp.manager.IPvpQueueManager
import com.smartfoxserver.v2.entities.data.ISFSObject

class PvpKeepJoiningQueueHandler : BaseEncryptRequestHandler() {
    override val serverCommand: String = SFSCommand.KEEP_JOINING_PVP_QUEUE_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        val pvpQueueManager = controller.svServices.get<IPvpQueueManager>()
        pvpQueueManager.keepJoining(controller.userName)
    }
}