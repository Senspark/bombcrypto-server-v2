package com.senspark.game.extension

import com.senspark.common.utils.ILogger
import com.senspark.game.pvp.manager.IPvpChatManager
import com.senspark.game.pvp.manager.IPvpLobbyStateManager
import com.smartfoxserver.v2.core.SFSEventType
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler
import com.smartfoxserver.v2.extensions.BaseServerEventHandler
import com.smartfoxserver.v2.extensions.SFSExtension
import com.smartfoxserver.v2.core.ISFSEvent

class PvpLobbyExtension : SFSExtension() {
    private val _logger: ILogger by lazy { PvpZoneExtension.AllServices.logger }
    private val _chatManager: IPvpChatManager by lazy { PvpZoneExtension.AllServices.chatManager }
    private val _lobbyStateManager: IPvpLobbyStateManager by lazy { PvpZoneExtension.AllServices.lobbyStateManager }

    override fun init() {
        addEventHandler(SFSEventType.USER_JOIN_ROOM, UserJoinLobbyHandler::class.java)
        addRequestHandler("chat", LobbyChatHandler::class.java)
        addRequestHandler("getStats", LobbyStatsHandler::class.java)
        
        _logger.log("[PvpLobbyExtension] Initialized for room: ${parentRoom.name}")
    }

    class UserJoinLobbyHandler : BaseServerEventHandler() {
        override fun handleServerEvent(event: ISFSEvent) {
            val extension = parentExtension as PvpLobbyExtension
            val user = event.getParameter(com.smartfoxserver.v2.core.SFSEventParam.USER) as User
            extension._logger.log("[PvpLobby] User ${user.name} joined lobby ${extension.parentRoom.name}")
            
            // Send initial stats to the user
            val stats = SFSObject()
            stats.putInt("onlinePlayers", extension._lobbyStateManager.getTotalPlayersOnline())
            stats.putInt("activeMatches", extension._lobbyStateManager.getActiveMatchesCount())
            extension.send("statsUpdate", stats, user)
        }
    }

    class LobbyChatHandler : BaseClientRequestHandler() {
        override fun handleClientRequest(user: User, params: ISFSObject) {
            val extension = parentExtension as PvpLobbyExtension
            val message = params.getUtfString("msg") ?: return

            if (extension._chatManager.canSendMessage(user)) {
                val filtered = extension._chatManager.filterMessage(message)
                val response = SFSObject()
                response.putUtfString("sender", user.name)
                response.putUtfString("msg", filtered)
                
                // Broadcast to all users in the lobby room
                extension.send("chat", response, extension.parentRoom.userList)
                extension._chatManager.onMessageSent(user)
            } else {
                // Send rate limit error
                val error = SFSObject()
                error.putInt("errorCode", 1000) // Replace with real error code
                extension.send("chatError", error, user)
            }
        }
    }

    class LobbyStatsHandler : BaseClientRequestHandler() {
        override fun handleClientRequest(user: User, params: ISFSObject) {
            val extension = parentExtension as PvpLobbyExtension
            val stats = SFSObject()
            stats.putInt("onlinePlayers", extension._lobbyStateManager.getTotalPlayersOnline())
            stats.putInt("activeMatches", extension._lobbyStateManager.getActiveMatchesCount())
            extension.send("statsUpdate", stats, user)
        }
    }
}
