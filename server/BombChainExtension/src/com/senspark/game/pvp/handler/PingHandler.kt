package com.senspark.game.pvp.handler

import com.senspark.common.pvp.IMatchController
import com.senspark.game.pvp.HandlerCommand
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.entities.data.ISFSObject

class PingHandler(
    private val _controller: IMatchController,
) : BasePVPEncryptRequestHandler() {
    override val command = HandlerCommand.PingPong
    override fun handleGameClientRequest(user: User, data: ISFSObject) {
        val timestamp = data.getLong("timestamp")
        val requestId = data.getInt("request_id")
        _controller.ping(user, timestamp, requestId)
    }
}