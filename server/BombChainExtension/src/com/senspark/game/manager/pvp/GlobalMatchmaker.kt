package com.senspark.game.manager.pvp

import com.senspark.common.cache.ICacheService
import com.senspark.common.cache.IMessengerService
import com.senspark.common.pvp.IMatchTeamInfo
import com.senspark.common.pvp.IMatchUserInfo
import com.senspark.common.pvp.PvpMode
import com.senspark.common.service.IScheduler
import com.senspark.common.utils.ILogger
import com.senspark.game.api.IPvpJoinQueueInfo
import com.senspark.game.api.redis.*
import com.senspark.game.constant.CachedKeys
import com.senspark.game.data.model.nft.ConfigHeroTraditional
import com.senspark.game.declare.ErrorCode
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.IEnvManager
import com.senspark.game.manager.IUsersManager
import com.senspark.game.pvp.info.*
import com.senspark.game.pvp.info.MatchHeroInfo
import com.senspark.game.pvp.manager.EpochTimeManager
import com.senspark.game.pvp.manager.ITimeManager
import com.senspark.game.pvp.utility.JsonUtility
import java.util.*
import kotlin.math.abs
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

class GlobalMatchmaker(
    private val _logger: ILogger,
    private val _envManager: IEnvManager,
    private val _scheduler: IScheduler,
    private val _timeManager: ITimeManager,
    private val _configs: List<ConfigHeroTraditional>,
    private val _listener: IMatchmakerListener,
    private val _messengerService: IMessengerService,
    private val _cache: ICacheService,
    private val _usersManager: IUsersManager,
) : IMatchmaker {
    private class QueueEntry(
        var timestamp: Long,
        val info: IPvpJoinQueueInfo,
    )

    companion object {
        private const val USER_JOIN_QUEUE_TTL_IN_MILLIS = 20 * 1000
        private const val SEND_USERS_IN_QUEUE_INTERVAL = 1 * 1000
    }

    private val _json = JsonUtility.json

    //private val _queueApi: IPvpQueueApi = PvpQueueApi(_envManager)
    private val _redisPvpQueueApi = RedisPvpQueueApi(_messengerService);

    private val _queueInfoLocker = Any()
    private val _queueInfoMap = mutableMapOf<String, QueueEntry>()

    init {
        _scheduler.schedule("PVPQueue_CheckTimeoutUsers", 0, USER_JOIN_QUEUE_TTL_IN_MILLIS, ::checkTimeoutUsers)
        //_scheduler.schedule("PVPQueue_SendUsersInQueue", 0, SEND_USERS_IN_QUEUE_INTERVAL, ::sendUsersInQueueToApi)
    }

    override fun join(info: IPvpJoinQueueInfo): Boolean {
        val entry = QueueEntry(
            timestamp = _timeManager.timestamp,
            info = info,
        )

        synchronized(_queueInfoLocker) {
            // Check if already joined.
            if (_queueInfoMap.containsKey(info.username)) {
                throw CustomException("User has joined already", ErrorCode.PVP_ALREADY_IN_QUEUE)
            }
            // Add to queue.
            _queueInfoMap[info.username] = entry

            // Send AP_PVP_LEAVE_QUEUE_STR
            val it = convertToPvpData(entry.info)
            _redisPvpQueueApi.joinQueue(it)

            return true
        }
    }

    override fun keepJoining(username: String) {
        synchronized(_queueInfoLocker) {
            val item = _queueInfoMap[username]
            if (item == null) {
                // May playWithBot already.
                _logger.log("[Pvp][GlobalMatchmaker:keepJoining] User $username is not in queue")
                return
            }
            item.timestamp = _timeManager.timestamp
        }
    }

    /** Sends all queued-users to api. */
    /*
    private fun sendUsersInQueueToApi() {
        val items: List<IPvpJoinQueueInfo>
        synchronized(_queueInfoLocker) {
            items = _queueInfoMap.map { it.value.info }
        }
        if (items.isEmpty()) {
            return
        }
        _queueApi.joinQueue(items)
        //val it = convertToPvpData(items[0])
        //_redisPvpQueueApi.joinQueue(it)
    }
    */

    private fun convertToPvpData(joinInfo: IPvpJoinQueueInfo): PvpData {
        val hero = PvpHeroInfo(
            joinInfo.info.hero.id,
            joinInfo.info.hero.color,
            joinInfo.info.hero.skin,
            joinInfo.info.hero.skinChests,
            joinInfo.info.hero.health,
            joinInfo.info.hero.speed,
            joinInfo.info.hero.damage,
            joinInfo.info.hero.bombCount,
            joinInfo.info.hero.bombRange,
            joinInfo.info.hero.maxHealth,
            joinInfo.info.hero.maxSpeed,
            joinInfo.info.hero.maxDamage,
            joinInfo.info.hero.maxBombCount,
            joinInfo.info.hero.maxBombRange,
        )
        val info = PvpDataInfo(
            joinInfo.info.serverId,
            joinInfo.info.matchId,
            joinInfo.info.mode,
            joinInfo.info.isBot,
            joinInfo.info.displayName,
            joinInfo.info.totalMatchCount,
            joinInfo.info.rank,
            joinInfo.info.point,
            joinInfo.info.boosters,
            joinInfo.info.availableBoosters,
            hero,
            joinInfo.info.avatar,
        )

        val pvpData = PvpData(
            joinInfo.username,
            joinInfo.pings,
            info,
            EpochTimeManager().timestamp,
            true
        )
        return pvpData
    }


    /** Check time-out users (users who join but not keep-joining). */
    private fun checkTimeoutUsers() {
        val timestamp = _timeManager.timestamp
        val timeOutUsers: List<String>
        synchronized(_queueInfoLocker) {
            timeOutUsers = _queueInfoMap.filter {
                it.value.timestamp + USER_JOIN_QUEUE_TTL_IN_MILLIS < timestamp
            }.keys.toList()
        }
        timeOutUsers.forEach { username ->
            if (_redisPvpQueueApi.leaveQueue(username)) {
                synchronized(_queueInfoLocker) {
                    _queueInfoMap.remove(username) // May failed if the user leave before.
                }
            } else {
                // FIXME: handle failed to leave queue.
            }
        }
    }

    override fun leave(username: String): Boolean {
        synchronized(_queueInfoLocker) {
            _queueInfoMap.remove(username)
            //Send AP_PVP_LEAVE_QUEUE_STR
            return _redisPvpQueueApi.leaveQueue(username) // Always true
        }
    }

    fun onMatchFound(
        id: String,
        serverId: String,
        serverDetail: String?,
        rule: IMatchRule,
        team: List<IMatchTeam>,
        data: List<IUser>
    ) {
        val ruleInfo = MatchRuleInfo(rule.roomSize, rule.teamSize, rule.canDraw, rule.round, rule.isTournament)
        val teamInfoList = mutableListOf<IMatchTeamInfo>()
        for ((index, it) in team.withIndex()) {
            teamInfoList.add(
                MatchTeamInfo(
                    it.slots
                )
            )
        }

        synchronized(_queueInfoLocker) {
            for ((index, user) in data.withIndex()) {
                if (user.data.isBot) {
                    // Set AES key cho bot
                    val userController = _usersManager.getUserController(user.id)
                    _cache.setToHash(
                        CachedKeys.AES_KEY,
                        "${user.id}_bot_$index",
                        Base64.getEncoder().encodeToString(userController?.userInfo?.aesKey?.encoded),
                        15.minutes
                    )
                    continue
                }

                // Kiểm tra tất cả username của match phải có trong _queueInfoMap.
                if (!_queueInfoMap.containsKey(user.id)) {
                    throw CustomException("Match already sent or user has already left this match $user.id")
                }
                // Remove from queue.
                _queueInfoMap.remove(user.id)
            }
        }

        data.forEachIndexed { idx, it ->
            _listener.onMatchFound(
                it.id,
                MatchInfo(
                    id = id,
                    serverId = serverId,
                    serverDetail = serverDetail ?: "",
                    timestamp = it.timestamp,
                    mode = PvpMode.fromValue(it.mode),
                    rule = ruleInfo,
                    team = teamInfoList,
                    slot = idx,
                    info = convertToMatchUserInfo(data)
                )
            )
        }
    }

    private fun convertToMatchUserInfo(data: List<IUser>): List<IMatchUserInfo> {
        val userList = mutableListOf<IMatchUserInfo>()
        for ((index, user) in data.withIndex()) {
            if (user.data.isBot) {
                // Generate bot users based on real users.
                val key = data
                    .filter { !it.data.isBot }
                    .joinToString { it.toString() }
                val seed = key.hashCode()
                val chars = ('a'..'f') + ('A'..'F') + ('0'..'9')
                val random = Random(seed)
                val configIndex = abs(random.nextInt()) % _configs.size
                val username = "${user.id}_bot_$index"
                val displayName = "0x${(1..40).map { chars.random(random) }.joinToString("")}"
                val matchUserInfo = generateBotConfig(configIndex, username, displayName)
                userList.add(matchUserInfo)
            } else {
                val matchUserInfo = generateConfig(user)
                userList.add(matchUserInfo)
            }
        }
        return userList
    }

    private fun generateConfig(user: IUser): IMatchUserInfo {
        val matchUserInfo = MatchUserInfo(
            serverId = user.serverId,
            buildVersion = 0,
            matchId = user.matchId?.toString().orEmpty(),
            mode = user.mode,
            isTest = false,
            isWhitelisted = false,
            isBot = user.data.isBot,
            userId = _usersManager.getUserId(user.id),
            username = user.id,
            displayName = user.data.displayName,
            totalMatchCount = user.totalMatchCount,
            matchCount = 0,
            winMatchCount = 0,
            rank = user.rank,
            point = user.point,
            boosters = user.data.boosters.toList(),
            availableBoosters = user.data.availableBoosters,
            hero = MatchHeroInfo(
                id = user.data.hero.heroId,
                color = user.data.hero.color,
                skin = user.data.hero.skin,
                skinChests = user.data.hero.skinChests,
                health = user.data.hero.health,
                speed = user.data.hero.speed,
                damage = user.data.hero.damage,
                bombCount = user.data.hero.bombCount,
                bombRange = user.data.hero.bombRange,
                maxHealth = user.data.hero.maxHealth,
                maxSpeed = user.data.hero.maxSpeed,
                maxDamage = user.data.hero.maxDamage,
                maxBombCount = user.data.hero.maxBombCount,
                maxBombRange = user.data.hero.maxBombRange,
            ),
            avatar = if (user.data.avatar == 0) -1 else user.data.avatar
        )
        return matchUserInfo
    }

    private fun generateBotConfig(index: Int, username: String, displayName: String): IMatchUserInfo {
        val config = _configs[index]
        val heroInfo = MatchHeroInfo(
            id = -1,
            color = config.color,
            skin = config.skin,
            skinChests = emptyMap(),
            health = config.hp,
            speed = config.speed,
            damage = config.dmg,
            bombCount = config.bomb,
            bombRange = config.range,
            maxHealth = config.maxHp,
            maxSpeed = config.maxSpeed,
            maxDamage = config.maxDmg,
            maxBombCount = config.maxBomb,
            maxBombRange = config.maxRange,
        )
        return MatchUserInfo(
            serverId = _envManager.serverId,
            buildVersion = 0,
            matchId = "",
            mode = 0,
            isTest = false,
            isWhitelisted = false,
            isBot = true,
            userId = -1,
            username = username,
            displayName = displayName,
            totalMatchCount = 0,
            matchCount = 0,
            winMatchCount = 0,
            rank = 1,
            point = 0,
            boosters = emptyList(),
            availableBoosters = emptyMap(),
            hero = heroInfo,
            avatar = -1
        )
    }
}