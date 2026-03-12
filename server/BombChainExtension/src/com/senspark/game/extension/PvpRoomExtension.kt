package com.senspark.game.extension

import com.senspark.common.pvp.IMatchController
import com.senspark.common.pvp.IMatchFactory
import com.senspark.common.pvp.IRoomExtension
import com.senspark.common.utils.ILogger
import com.senspark.game.pvp.handler.UserJoinRoomHandler
import com.senspark.game.pvp.handler.UserLeaveRoomHandler
import com.smartfoxserver.v2.core.SFSEventType
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.extensions.SFSExtension

class PvpRoomExtension : SFSExtension(), IRoomExtension {
    private lateinit var _logger: ILogger
    private lateinit var _factory: IMatchFactory

    override val controller: IMatchController get() = _factory.controller

    override fun init() {
        // No-op.
    }

    override fun initialize(factory: IMatchFactory, logger: ILogger) {
        _logger = logger
        _factory = factory

        factory.initialize(logger)

        // System events.
        addEventHandler(SFSEventType.USER_JOIN_ROOM, UserJoinRoomHandler(logger, controller))
        addEventHandler(SFSEventType.USER_LEAVE_ROOM, UserLeaveRoomHandler(logger, controller))

        // Extension requests.
        val handlers = factory.handlers
        handlers.forEach {
            addRequestHandler(it.command, it)
        }
        logger.log("[Pvp][RoomExtension:initialize] ${controller.matchInfo.id}")
    }

    override fun join(user: User, isObserver: Boolean) {
        api.joinRoom(user, parentRoom, "", isObserver, user.lastJoinedRoom)
    }

    override fun leave(user: User) {
        api.leaveRoom(user, parentRoom)
    }

    override fun kick(user: User, reason: String) {
        api.kickUser(user, null, reason, 2)
    }

    override fun release() {
        _logger.log("[Pvp][RoomExtension:release] ${controller.matchInfo.id}")
        parentRoom.userList.forEach {
            try {
                api.disconnectUser(it)
            } catch (e: Exception) {
                _logger.error("Disconnect user error: ${e.message}")
                _logger.error(e)
            }
        }
        api.removeRoom(parentRoom)
    }

    override fun destroy() {
        _logger.log("[Pvp][RoomExtension:destroy] ${controller.matchInfo.id}")
        _factory.destroy()
        super.destroy()
    }
}