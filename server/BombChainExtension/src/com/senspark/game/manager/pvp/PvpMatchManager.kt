package com.senspark.game.manager.pvp

import com.senspark.common.cache.ICacheService
import com.senspark.common.cache.IMessengerService
import com.senspark.common.pvp.*
import com.senspark.common.service.IScheduler
import com.senspark.common.utils.ILogger
import com.senspark.game.api.IPvpResultInfo
import com.senspark.game.api.PvpReportApi
import com.senspark.game.api.PvpResultInfo
import com.senspark.game.constant.CachedKeys
import com.senspark.game.constant.StreamKeys.Companion.SV_PVP_MATCH_FINISHED_STR
import com.senspark.game.extension.PvpRoomExtension
import com.senspark.game.manager.IPvpEnvManager
import com.senspark.game.pvp.config.ConstantMapConfig
import com.senspark.game.pvp.entity.UserPvpProperty
import com.senspark.game.pvp.handler.*
import com.senspark.game.pvp.info.IMatchHistoryInfo
import com.senspark.game.pvp.info.MatchInfo
import com.senspark.game.pvp.manager.*
import com.senspark.game.service.IPvpDataAccess
import com.senspark.game.utils.ISender
import com.senspark.game.utils.SafeScheduler
import com.senspark.game.service.*
import com.senspark.game.declare.ErrorCode
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.IUsersManager
import com.senspark.game.pvp.config.PvpWagerTier
import com.senspark.game.pvp.config.PvpWagerToken
import com.smartfoxserver.v2.api.CreateRoomSettings
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode
import com.smartfoxserver.v2.entities.User
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ScheduledThreadPoolExecutor
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

private interface IMatchEntry {
    val startTimestamp: Long
    val isFinished: Boolean
    fun join(user: User)
    fun leave(user: User)
    fun finish(info: IPvpResultInfo)
}

private class MatchEntry(
    override val startTimestamp: Long,
    private val _extension: IRoomExtension,
    private val _messageBridge: IMessageBridge,
) : IMatchEntry {

    private val _locker = Any()
    private var _finished = false
    private lateinit var _resultInfo: IPvpResultInfo

    override val isFinished: Boolean
        get() {
            synchronized(_locker) {
                return _finished
            }
        }

    override fun join(user: User) {
        synchronized(_locker) {
            if (_finished) {
                _messageBridge.finishMatch(_resultInfo, listOf(user))
            } else {
                // Let this user join the match.
                val info = user.getJoinPVPMatchInfo()
                if (info.slot < info.info.size) {
                    // Participant.
                    _extension.join(user, false)
                } else {
                    // Observer.
                    _extension.join(user, true)
                }
            }
        }
    }

    override fun leave(user: User) {
        synchronized(_locker) {
            if (_finished) {
                return@synchronized
            }
            _extension.leave(user)
        }
    }

    override fun finish(info: IPvpResultInfo) {
        synchronized(_locker) {
            _finished = true
            _resultInfo = info
            _extension.release()
        }
    }
}

class PvpMatchManager(
    private val _databaseManager: IDatabaseManager,
    private val _scheduler: IScheduler,
    private val _sender: ISender,
    private val _roomCreator: IRoomCreator,
    private val _timeManager: ITimeManager,
    private val _matchInfoBroadcaster: MatchInfoUpdatedBroadcaster,
    private val _envManager: IPvpEnvManager,
    private val _logger: ILogger,
    private val _messengerService: IMessengerService,
    private val _cache: ICacheService,
    private val _pvpDataAccess: IPvpDataAccess,
    private val _pvpRankManager: IRankManager,
    private val _wagerService: IPvpWagerService,
    private val _usersManager: IUsersManager,
    private val _rankingService: IPvpRankingService,
) : IMatchManager {
    companion object {
        private const val MATCH_EXPIRY_TIME_IN_MILLIS = 300 * 1000 // 5 minutes per match token.
    }

    private val _messageBridge = DefaultMessageBridge(_logger, _sender, _cache)
    private val _matchMapLocker = Any()
    private val _matchMap = mutableMapOf<String, IMatchEntry>()
    private val _reportApi = PvpReportApi(_envManager)
    //private val _resultApi = PvpResultApi(_logger, _envManager)

    init {
        _scheduler.schedule("PvpMatchManager_update", 0, 1000) {
            val now = _timeManager.timestamp
            synchronized(_matchMapLocker) {
                _matchMap.entries.removeIf { (id, entry) ->
                    if (!entry.isFinished) {
                        return@removeIf false
                    }
                    val duration = now - entry.startTimestamp
                    if (duration < MATCH_EXPIRY_TIME_IN_MILLIS) {
                        return@removeIf false
                    }
                    _logger.log("[Pvp][PvpMatchManager:update] remove id=$id")
                    true
                }
            }
        }
        _scheduler.schedule("MatchInfoBroadcaster", 0, 5 * 1000) {
            _matchInfoBroadcaster.broadcast()
        }
    }

    override fun validate(info: IMatchInfo, hash: String) {
        _logger.log("[Pvp][PvpMatchManager:validate] info=$info")
        // Compute hash from the specified info and compare with the current hash.
        val expectedHash = info.hash

        // Comparison.
        // FIXME: Tạm thời disable, fix ở bản sau
//        if (hash != expectedHash) {
//            throw InvalidMatchHashException("Wrong match info hash, found $hash expected $expectedHash")
//        }

        val now = _timeManager.timestamp
        val expiryTime = info.timestamp + MATCH_EXPIRY_TIME_IN_MILLIS
        if (now >= expiryTime) {
            throw MatchExpiredException()
        }
    }

    override fun join(user: User) {
        _logger.log("[Pvp][PvpMatchManager:join] user=$user")
        val info = user.getJoinPVPMatchInfo()
        
        // FASE 2: Escrow Debit
        if (info.wagerMode == 1) {
            val tier = PvpWagerTier.from(info.wagerTier)
            val token = PvpWagerToken.from(info.wagerToken)
            val userId = _usersManager.getUserId(user.name)
            if (userId == -1) {
                throw CustomException("User not found: ${user.name}", ErrorCode.USER_NOT_FOUND)
            }
            
            if (!_wagerService.debitEscrow(info.id, userId, tier, token)) {
                throw CustomException("Insufficient funds for wager", ErrorCode.PVP_JOIN_FAILED)
            }
            _logger.log("[Pvp][Wager] Debited escrow for user=$userId, match=${info.id}, amount=${tier.amount}")
        }

        val aesKey = getAESKey(user.name)
        user.setProperty(UserPvpProperty.AES_KEY, aesKey)
        synchronized(_matchMapLocker) {
            val item = _matchMap.getOrPut(info.id) {
                if (info.slot == info.info.size) {
                    // Observer join first.
                    require(false) { "Observer cannot create a room" }
                }
                // Create room immediately.
                val extension = createRoom(info)
                MatchEntry(info.timestamp, extension, _messageBridge)
            }
            item.join(user)
        }
    }

    override fun leave(user: User) {
        _logger.log("[Pvp][PvpMatchManager:leave] user=$user")
        val info = user.getJoinPVPMatchInfo()
        synchronized(_matchMapLocker) {
            val item = _matchMap[info.id] ?: throw Exception("Cannot find match ${info.id}")
            item.leave(user)
        }
    }

    override fun finish(
        resultInfo: IPvpResultInfo,
        historyInfo: IMatchHistoryInfo,
        stats: IMatchStats,
    ) {
        _logger.log("[Pvp][PvpMatchManager:finish] id=${resultInfo.id}")

        // Send result to redis, not send directly to the original server.
        _messengerService.send(SV_PVP_MATCH_FINISHED_STR, PvpResultInfo.parse(resultInfo))
        // Send result to original servers.
        //_resultApi.send(resultInfo)


        // Report.
        _reportApi.report(historyInfo)

        // Database.
        _databaseManager.addMatch(resultInfo, stats)

        // delay 2 giây để tránh trường hợp client nhận PVP_FINISH_MATCH sau entry.finish
        CoroutineScope(Dispatchers.Default).launch {
            delay(2000)

            synchronized(_matchMapLocker) {
                val entry = _matchMap[resultInfo.id] ?: return@synchronized
                
                // FASE 2: Prize Distribution
                if (resultInfo.wagerMode == 1) {
                    try {
                        _wagerService.distributePrize(resultInfo)
                        _logger.log("[Pvp][Wager] Distributed prizes for match=${resultInfo.id}")
                    } catch (e: Exception) {
                        _logger.log("[Pvp][Wager][ERROR] Failed to distribute prize for match=${resultInfo.id}: ${e.message}")
                    }
                }

                _logger.log("[Pvp][MatchEntry:finish] id=${resultInfo.id}")
                
                // FASE 5: Ranking Update
                try {
                    _rankingService.updateRankings(resultInfo)
                    _logger.log("[Pvp][Ranking] Updated rankings for match=${resultInfo.id}")
                } catch (e: Exception) {
                    _logger.log("[Pvp][Ranking][ERROR] Failed to update rankings for match=${resultInfo.id}: ${e.message}")
                }

                // Must call finish latest, this will also destroy the current thread.
                entry.finish(resultInfo)
            }
        }
    }

    private fun createRoom(info: IMatchInfo): IRoomExtension {
        require(info.rule.round > 0) { "Invalid number of round=${info.rule.round}" }
        val observerInfo = MatchInfo(
            id = info.id,
            serverId = info.serverId,
            serverDetail = info.serverDetail,
            timestamp = info.timestamp,
            mode = info.mode,
            rule = info.rule,
            team = info.team,
            slot = info.info.size,
            info = info.info,
            wagerMode = info.wagerMode,
            wagerTier = info.wagerTier,
            wagerToken = info.wagerToken,
        )
        val settings = CreateRoomSettings().apply {
            groupId = "pvp"
            maxUsers = info.info.size
            maxSpectators = 10
            name = info.id
            autoRemoveMode = SFSRoomRemoveMode.WHEN_EMPTY
            extension =
                CreateRoomSettings.RoomExtensionSettings(
                    "PVPZoneExtension", // Directory.
                    PvpRoomExtension::class.java.name
                )
            isDynamic = true
            isGame = true
        }
        val room = _roomCreator.createRoom(settings)
        val extension = room.extension as IRoomExtension
        extension.initialize(object : IMatchFactory {
            private val _executor = ScheduledThreadPoolExecutor(2)
            private lateinit var _scheduler: IScheduler
            private lateinit var _controller: IMatchController
            private lateinit var _handlers: List<RoomHandler>

            override val controller get() = _controller
            override val handlers get() = _handlers

            override fun initialize(logger: ILogger) {
                // Force MatchController created in the same class loader with PvpZoneExtension.
                _scheduler = SafeScheduler(logger, DefaultScheduler(logger, _executor))
                _controller = PvpMatchController(
                    observerInfo,
                    settings.maxSpectators,
                    _timeManager.timestamp,
                    logger,
                    this@PvpMatchManager,
                    _messageBridge,
                    createMapGenerator(info.mode),
                    _pvpRankManager,
                    _scheduler
                )
                _controller.initialize()
                val dispatcher = _executor.asCoroutineDispatcher()
                val scope = CoroutineScope(dispatcher)
                _handlers = listOf(
                    ReadyHandler(controller),
                    QuitHandler(controller),
                    PingHandler(controller),
                    MoveHeroHandler(scope, controller),
                    PlantBombHandler(scope, controller),
                    ThrowBombHandler(scope, controller),
                    UseBoosterHandler(scope, controller),
                    UseEmojiHandler(controller),
                )
            }

            override fun destroy() {
                _scheduler.clearAll()
                _executor.shutdownNow()
            }
        }, _logger)
        return extension
    }

    private fun createMapGenerator(mode: PvpMode): IMapGenerator {
        val mapConfig = DynamicMapConfig(mode)
        val blockHealthManager = DefaultBlockHealthManager(_pvpDataAccess.queryPvPBlockHealth())
        val chestDropRate = _pvpDataAccess.queryPvPChestDropRate()
        val itemDropRate = _pvpDataAccess.queryPvPItemDropRate()
        return PvpMapGenerator(
            mapConfig,
            _pvpDataAccess.queryPvPChestSpawnRadius(),
            _pvpDataAccess.queryPvPChestDensity(),
            chestDropRate.keys.toList(),
            chestDropRate.values.toList(),
            mapConfig.itemDensity,
            itemDropRate.keys.toList(),
            itemDropRate.values.toList(),
            MapBlockGenerator(blockHealthManager, mapConfig.blockDensity),
            _logger,
        )
    }
    
    private fun getAESKey(userName: String): SecretKey {
        val encodedKey = _cache.getFromHash(CachedKeys.AES_KEY, userName) ?: throw Exception("Could not find user's key")
        val aesKey = Base64.getDecoder().decode(encodedKey)
        return SecretKeySpec(aesKey, 0, aesKey.size, "AES")
    }
}

fun User.getJoinPVPMatchInfo(): IMatchInfo =
    session.getProperty(MatchInfo.PROPERTY_KEY) as IMatchInfo