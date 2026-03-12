package com.senspark.game.pvp.manager

import com.senspark.common.cache.ICacheService
import com.senspark.common.utils.ILogger
import com.senspark.game.constant.CachedKeys
import com.senspark.game.manager.IPvpEnvManager
import com.smartfoxserver.v2.SmartFoxServer

interface IServerInfoManager {
    fun updateUserOnlineToRedis()
    fun getServerInfoTimeUpdate(): Int
}

object Constant {
    const val GAME_ZONE = "BomberGameZone"
    const val PVP_ZONE = "PVPZone"
    const val API_ZONE = "APIZone"
}

class ServerInfoManager (
    private val _envManager: IPvpEnvManager,
    private val _cacheService: ICacheService, 
    private val _logger: ILogger,
): IServerInfoManager {

    private val _timeUpdate = 20 //seconds

    override fun updateUserOnlineToRedis() {
        try {
            val extension = SmartFoxServer.getInstance().zoneManager?.getZoneByName(Constant.PVP_ZONE)
            val userCount = extension?.userCount
            val name = _envManager.serverId
            // Send userCount to Redis
            _cacheService.setToHash(CachedKeys.SV_SERVER_PVP_INFO, name, userCount.toString())
        } catch (e: Exception) {
            _logger.error("Error when update user online to redis: ${e.message}")
        }

    }

    override fun getServerInfoTimeUpdate(): Int {
        return _timeUpdate
    }

}