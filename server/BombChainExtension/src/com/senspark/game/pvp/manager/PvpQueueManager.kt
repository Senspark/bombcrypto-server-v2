package com.senspark.game.pvp.manager

import com.senspark.common.cache.ICacheService
import com.senspark.common.cache.IMessengerService
import com.senspark.common.pvp.IMatchInfo
import com.senspark.common.pvp.IMatchUserInfo
import com.senspark.common.service.IScheduler
import com.senspark.common.utils.ILogger
import com.senspark.game.api.PvpJoinQueueInfo
import com.senspark.game.data.manager.hero.IConfigHeroTraditionalManager
import com.senspark.game.manager.IEnvManager
import com.senspark.game.manager.IUsersManager
import com.senspark.game.manager.pvp.GlobalMatchmaker
import com.senspark.game.manager.pvp.LocalMatchmaker
import com.senspark.game.service.IPvpDataAccess
import com.senspark.game.service.IPvpWagerService
import com.senspark.game.pvp.HandlerCommand
import com.senspark.game.pvp.info.MatchInfoClient
import com.senspark.game.pvp.info.MatchRuleInfoClient
import com.senspark.game.pvp.utility.JsonUtility
import com.senspark.game.utils.ISender
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import javax.crypto.SecretKey

class PvpQueueManager(
    private val _logger: ILogger,
    private val _envManager: IEnvManager,
    private val _scheduler: IScheduler,
    private val _messengerService: IMessengerService,
    private val _cache: ICacheService,
    private val _configHeroTraditionalManager: IConfigHeroTraditionalManager,
    private val _sender: ISender,
    private val _usersManager: IUsersManager,
    private val _pvpDataAccess: IPvpDataAccess,
    private val _wagerService: IPvpWagerService,
) : IPvpQueueManager {
    private val _json = JsonUtility.json
    private val _timeManager: ITimeManager = EpochTimeManager()
    private val _localMatchmaker = LocalMatchmaker()
    private lateinit var _globalMatchmaker: GlobalMatchmaker
    private val _locker = Any()
    private val _userMappers = mutableMapOf<String, Pair<User, SecretKey>>()

    override val matchMaker: GlobalMatchmaker
        get() {
            return _globalMatchmaker
        }

    override fun initialize() {
        _globalMatchmaker = GlobalMatchmaker(
            _logger,
            _envManager,
            _scheduler,
            _timeManager,
            _configHeroTraditionalManager.getAllConfigs().filter { it.canBeBot },
            this,
            _messengerService,
            _cache,
            _usersManager,
            _pvpDataAccess,
            _wagerService
        )
    }

    override fun join(
        user: User,
        username: String,
        pings: Map<String, Int>,
        info: IMatchUserInfo,
        aesKey: SecretKey,
        gameMode: Int,
        wagerMode: Int,
        wagerTier: Int,
        wagerToken: Int,
        network: String
    ): Boolean {
        _logger.log("[Pvp][PvpQueueManager:join] username=${username} gameMode=$gameMode wagerMode=$wagerMode wagerTier=$wagerTier wagerToken=$wagerToken")
        // Join-queue info.
        val queueInfo = PvpJoinQueueInfo(
            username = username,
            pings = pings,
            info = info, // Aux data.
            gameMode = gameMode,
            wagerMode = wagerMode,
            wagerTier = wagerTier,
            wagerToken = wagerToken,
            network = network
        )
        synchronized(_locker) {
            _userMappers[username] = Pair(user, aesKey)
        }
        if (info.isTest) {
            return _localMatchmaker.join(queueInfo)
        }
        return _globalMatchmaker.join(queueInfo)
    }

    override fun keepJoining(username: String) {
        _globalMatchmaker.keepJoining(username)
    }

    override fun leave(username: String): Boolean {
        return _globalMatchmaker.leave(username)
    }

    override fun destroy() {}

    override fun onMatchFound(
        username: String,
        info: IMatchInfo,
    ) {
        val user = find(username)
        require(user != null) { "Cannot find user $username" }
        // Temporarily convert variables for legacy client compatibility. This will be removed in future updates.
        val infoClient = MatchInfoClient(
            id = info.id,
            serverId = info.serverId,
            serverDetail = info.serverDetail,
            timestamp = info.timestamp,
            mode = info.mode,
            rule = MatchRuleInfoClient(
                room_size = info.rule.roomSize,
                team_size = info.rule.teamSize,
                can_draw = info.rule.canDraw,
                round = info.rule.round,
                is_tournament = info.rule.isTournament,
                game_mode = info.rule.gameMode,
                wager_mode = info.rule.wagerMode,
                wager_tier = info.rule.wagerTier,
                wager_token = info.rule.wagerToken
            ),
            team = info.team,
            slot = info.slot,
            info = info.info,
        )

        // Send data back to client to enter PVP
        val data = buildJsonObject {
            put("info", _json.encodeToJsonElement(infoClient).apply {
                put("hash", info.hash)
            })
            put("hash", info.hash)
            put("code", 0)
        }
        val params = SFSObject.newFromJsonData(_json.encodeToString(data))
        _sender.sendWithEncrypt(HandlerCommand.FoundMatch, params, user.first, false, user.second)
    }

    override fun find(username: String): Pair<User, SecretKey>? {
        synchronized(_locker) {
            if (!_userMappers.containsKey(username)) {
                return null
            }
            return _userMappers[username]
        }
    }
}