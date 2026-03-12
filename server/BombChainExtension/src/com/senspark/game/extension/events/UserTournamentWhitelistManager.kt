package com.senspark.game.extension.events

import com.senspark.common.cache.ICacheService
import com.senspark.common.service.IServerService
import com.senspark.common.service.IService
import com.senspark.common.utils.AppStage
import com.senspark.common.utils.ILogger
import com.senspark.game.constant.CachedKeys
import com.senspark.game.db.IPvpTournamentDataAccess
import com.senspark.game.manager.IEnvManager
import kotlin.time.Duration.Companion.seconds

interface IUserTournamentWhitelistManager : IService, IServerService {
    fun isInWhitelist(userId: String): Boolean
}

class UserTournamentWhitelistManager(
    private val _pvpTournamentDataAccess: IPvpTournamentDataAccess,
    private val _envManager: IEnvManager,
    private val _cacheService: ICacheService,
    private val _logger: ILogger,
) : IUserTournamentWhitelistManager {

    companion object {
        const val CACHE_SECONDS_TEST = 5 // 5 seconds
        const val CACHE_SECONDS_PROD = 60 * 3 // 3 minute
    }

    override fun initialize() {
    }

    override fun isInWhitelist(userId: String): Boolean {
        val set = getWhitelist()
        if (set.isEmpty()) {
            return false
        }
        if (set.contains(userId.lowercase())) {
            return true
        }
        return false
    }

    override fun destroy() {
    }

    private fun getWhitelist(): Set<String> {
        try {
            var whiteList = _cacheService.get(CachedKeys.SV_TOURNAMENT_WHITELIST)
            if (whiteList.isNullOrEmpty()) {
                val users = _pvpTournamentDataAccess.getTournamentUsers()
                whiteList = if (users.isNotEmpty()) users.joinToString(",").lowercase() else "_"
                _cacheService.set(CachedKeys.SV_TOURNAMENT_WHITELIST, whiteList, getCacheSeconds().seconds)
                return users
            } else {
                return whiteList.split(",").toSet()
            }
        } catch (e: Exception) {
            _logger.error(e)
            return emptySet()
        }
    }

    private fun getCacheSeconds(): Int {
        return if (_envManager.appStage == AppStage.PROD) {
            CACHE_SECONDS_PROD
        } else {
            CACHE_SECONDS_TEST
        }
    }
}