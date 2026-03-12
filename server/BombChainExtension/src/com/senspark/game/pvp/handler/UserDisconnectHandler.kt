package com.senspark.game.pvp.handler

import com.senspark.common.utils.ILogger
import com.senspark.game.extension.PvpZoneExtension
import com.senspark.game.pvp.manager.IMatchManager
import com.smartfoxserver.v2.core.ISFSEvent
import com.smartfoxserver.v2.core.SFSEventParam
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.extensions.BaseServerEventHandler

class UserDisconnectHandler : BaseServerEventHandler() {

    private val _logger: ILogger = PvpZoneExtension.AllServices.logger
    private val _matchManager: IMatchManager = PvpZoneExtension.AllServices.matchManager
    
    override fun handleServerEvent(event: ISFSEvent) {
        try {
            val user = event.getParameter(SFSEventParam.USER) as User
            _matchManager.leave(user)
        } catch (ex: Exception) {
            _logger.log(ex.stackTraceToString())
        }
    }
}