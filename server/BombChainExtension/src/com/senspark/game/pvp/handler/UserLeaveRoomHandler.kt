package com.senspark.game.pvp.handler

import com.senspark.common.pvp.IMatchController
import com.senspark.common.utils.ILogger
import com.smartfoxserver.v2.core.ISFSEvent
import com.smartfoxserver.v2.core.SFSEventParam
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.extensions.BaseServerEventHandler

class UserLeaveRoomHandler(
    private val _logger: ILogger,
    private val _controller: IMatchController,
) : BaseServerEventHandler() {
    override fun handleServerEvent(event: ISFSEvent) {
        try {
            val user = event.getParameter(SFSEventParam.USER) as User
            _controller.leaveRoom(user)
        } catch (ex: Exception) {
            _logger.log(ex.stackTraceToString())
        }
    }
}