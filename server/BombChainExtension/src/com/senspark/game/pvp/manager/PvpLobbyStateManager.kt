package com.senspark.game.pvp.manager

import com.senspark.common.cache.ICacheService
import com.senspark.common.utils.ILogger
import com.senspark.game.constant.CachedKeys
import com.senspark.game.manager.IPvpEnvManager

interface IPvpLobbyStateManager {
    fun getTotalPlayersOnline(): Int
    fun getActiveMatchesCount(): Int
}

class PvpLobbyStateManager(
    private val _cacheService: ICacheService,
    private val _logger: ILogger
) : IPvpLobbyStateManager {

    override fun getTotalPlayersOnline(): Int {
        return try {
            val serverInfos = _cacheService.getAllFromHash(CachedKeys.SV_SERVER_PVP_INFO)
            serverInfos.values.sumOf { it.toIntOrNull() ?: 0 }
        } catch (e: Exception) {
            _logger.error("Error getting total players online: ${e.message}")
            0
        }
    }

    override fun getActiveMatchesCount(): Int {
        // This could be tracked in Redis under a specific key when matches start/finish
        // For now, let's return a mock or a simple estimate from another Redis key
        return try {
             _cacheService.getAllFromHash(CachedKeys.SV_PVP_MATCH_ACTIVE_COUNT).size
        } catch (e: Exception) {
            0
        }
    }
}
