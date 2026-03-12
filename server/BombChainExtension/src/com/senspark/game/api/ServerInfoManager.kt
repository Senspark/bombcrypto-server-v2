package com.senspark.game.api

import com.senspark.common.cache.ICacheService
import com.senspark.common.service.IServerService
import com.senspark.common.utils.ILogger
import com.senspark.game.constant.CachedKeys
import com.senspark.game.declare.EnumConstants
import com.senspark.game.manager.IEnvManager
import com.senspark.game.manager.treasureHuntV2.THModeV2Room.Companion.ROOM_NAME
import com.senspark.lib.data.manager.IGameConfigManager
import com.smartfoxserver.v2.SmartFoxServer
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.Serializable

@Serializable
data class ServerInfoResponse(
    val serverName: String,
    val users: Int,
)

interface IServerInfoManager : IServerService {
    fun getServerInfo(): ISFSObject
    fun getServerInfoTimeUpdate(): Int
    fun reloadUserOnline()
    fun isEnable(): Boolean
}

object Constant {
    const val GAME_ZONE = "BomberGameZone"
    const val PVP_ZONE = "PVPZone"
    const val API_ZONE = "APIZone"
}


class ServerInfoManager(
    private val _cacheService: ICacheService,
    private val _envManager: IEnvManager,
    private val _logger: ILogger,
    private val _gameConfig: IGameConfigManager,
) : IServerInfoManager {

    private val _timeUpdate = 20 //seconds

    private var serverInfoCache = mutableMapOf<String, ServerInfoResponse>()
    private var response: ISFSObject = SFSObject()

    override fun initialize() {
    }

    override fun reloadUserOnline() {
        if (!isEnable())
            return

        try {
            val extension = SmartFoxServer.getInstance().zoneManager?.getZoneByName(Constant.GAME_ZONE)
            var userCount = 0
            var userThModeCount = 0
            if (extension != null) {
                userCount = extension.userCount
                userThModeCount = extension.roomManager.getRoomByName(ROOM_NAME).size.userCount
            }
            serverInfoCache.clear()
            // Lưu số user đang online server smartfox
            val serverMain = EnumConstants.ZoneName.ServerMain.name
            serverInfoCache[serverMain] = ServerInfoResponse(serverMain, userCount)

            // Số user đang online trong TH mode
            val ThMode = EnumConstants.ZoneName.ThMode.name
            serverInfoCache[ThMode] = ServerInfoResponse(ThMode, userThModeCount)

            // Số user đang online trong 5 server Pvp
            val serverPvp = EnumConstants.ZoneName.ServerPvp.name
            val allValues = _cacheService.getAllFromHash(CachedKeys.SV_SERVER_PVP_INFO)
            val users = allValues.values.sumOf { it?.toIntOrNull() ?: 0 }
            serverInfoCache[serverPvp] = ServerInfoResponse(serverPvp, users)

            updateDataResponse()

        } catch (e: Exception) {
            _logger.error("Error reloadUserOnline: ${e.message}")
        }
    }

    override fun isEnable(): Boolean {
        //server Ton disable tính năng này
        if (_envManager.isTonServer && !_gameConfig.enableGetServerInfoTon)
            return false

        //server web disable tính năng này
        if (_envManager.isGameServer && !_gameConfig.enableGetServerInfoWeb)
            return false

        return true
    }

    override fun getServerInfo(): ISFSObject {
        return response
    }

    private fun updateDataResponse() {
        val serverInfoArray = SFSArray()

        serverInfoCache.forEach { (key, serverInfoResponse) ->
            val serverInfo = SFSObject()
            serverInfo.putUtfString("name", serverInfoResponse.serverName)
            serverInfo.putInt("users", serverInfoResponse.users)
            serverInfoArray.addSFSObject(serverInfo)
        }

        response.putSFSArray("server_info", serverInfoArray)
    }

    override fun getServerInfoTimeUpdate(): Int {
        return _timeUpdate
    }
}