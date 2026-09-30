package com.senspark.game.pvp.manager

import com.senspark.common.pvp.*
import com.senspark.common.service.IScheduler
import com.senspark.common.utils.ILogger
import com.senspark.game.api.*
import com.senspark.game.constant.Booster
import com.senspark.game.constant.ItemType
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.manager.pvp.getJoinPVPMatchInfo
import com.senspark.game.pvp.PrefixLogger
import com.senspark.game.pvp.config.IHeroConfig
import com.senspark.game.pvp.data.*
import com.senspark.game.pvp.entity.*
import com.senspark.game.pvp.info.*
import com.senspark.game.pvp.user.IObserverController
import com.senspark.game.pvp.user.IParticipantController
import com.senspark.game.pvp.user.ObserverController
import com.senspark.game.pvp.user.ParticipantController
import com.senspark.game.pvp.utility.DefaultRandom
import com.senspark.game.pvp.utility.IRandom
import com.smartfoxserver.bitswarm.sessions.SessionType
import com.smartfoxserver.v2.core.SFSConstants
import com.smartfoxserver.v2.entities.User
import org.koin.core.component.KoinComponent

private class MatchManagerInfo(
    val match: IMatch,
    val mapInfo: IMapInfo,
    val timeManager: StepTimeManager,
    val stateManager: IStateManager,
    val dataFactory: IObserveDataFactory,
    val commandManager: ICommandManager,
    val reportManager: IReportManager,
    val updater: IUpdater,
)

private class ResultInfo(
    val isFinished: Boolean,
    val isDraw: Boolean,
    val winningTeam: Int,
)

class PvpMatchController(
    private val _matchInfo: IMatchInfo,
    maxObservers: Int,
    startTimestamp: Long,
    logger: ILogger,
    private val _matchManager: IMatchManager,
    private val _messageBridge: IMessageBridge,
    private val _mapGenerator: IMapGenerator,
    private val _rankManager: IRankManager,
    private val _scheduler: IScheduler,
) : IMatchController, KoinComponent {
    /** Config. */
    private val _readyTimeOutDuration = 30000
    private val _stepDuration = 16

    /** Services. */
    private val _timeManager: ITimeManager = EpochTimeManager()

    /** Lockers. */
    private val _stepLocker = Any()
    private val _statusLocker = Any()
    private val _readyLocker = Any()
    private val _participantLocker = Any()
    private val _observerLocker = Any()

    private val _logger = PrefixLogger("[Pvp][${_matchInfo.id}]", logger)
    private val _random = DefaultRandom(startTimestamp)

    /**
     * Participant controllers.
     * NOTE: must have fixed amount and data is persisted throughout the match.
     */
    private val _participantControllers: List<IParticipantController> = _matchInfo.info.mapIndexed { index, info ->
        ParticipantController(
            info = info,
            teamId = _matchInfo.team.indexOfFirst { it.slots.contains(index) },
            _slot = index,
            _logger = _logger,
            _timeManager = _timeManager,
        )
    }

    /**
     * Observer controllers.
     * NOTE: may be added/removed flexibly.
     */
    private val _observerControllers: List<IObserverController> = (0 until maxObservers).map {
        ObserverController(it, _logger)
    }

    /** All controllers. */
    private val _controllers = _participantControllers + _observerControllers

    private val _networkManager: INetworkManager =
        DefaultNetworkManager(
            _participantControllers,
            _observerControllers,
            _logger,
            _messageBridge,
            _timeManager,
            300,
            30,
            5
        )
    private val _packetManager: IPacketManager = DefaultPacketManager()

    private val _matchData: IMatchData = MatchData(
        id = _matchInfo.id,
        status = MatchStatus.MatchStarted,
        observerCount = 0,
        startTimestamp = startTimestamp,
        readyStartTimestamp = 0,
        roundStartTimestamp = 0,
        round = 0,
        results = mutableListOf(),
    )
    private val _heroListener: IHeroListener
    private val _bombListener: IBombListener
    private val _mapListener: IMapListener

    private val _joinedSlots = mutableSetOf<Int>()
    private var _networkStepDuration = 0

    private lateinit var _match: IMatch
    private lateinit var _mapInfo: IMapInfo
    private lateinit var _matchTimeManager: StepTimeManager
    private lateinit var _stateManager: IStateManager
    private lateinit var _dataFactory: IObserveDataFactory
    private lateinit var _commandManager: ICommandManager
    private lateinit var _reportManager: IReportManager
    private lateinit var _updater: IUpdater
    private lateinit var _matchResult: IPvpResultInfo

    private val activeUsers get() = _controllers.mapNotNull { it.user }

    override val matchInfo get() = _matchInfo
    override val matchData: IMatchData
        get() {
            synchronized(_statusLocker) {
                return MatchData(
                    id = _matchData.id,
                    status = _matchData.status,
                    observerCount = _matchData.observerCount,
                    startTimestamp = _matchData.startTimestamp,
                    readyStartTimestamp = _matchData.readyStartTimestamp,
                    roundStartTimestamp = _matchData.roundStartTimestamp,
                    round = _matchData.round,
                    results = _matchData.results
                )
            }
        }
    override val matchStats: IMatchStats
        get() {
            val userCount = matchInfo.info.size
            return MatchStats(
                userStats = (0 until userCount).map {
                    val user = _participantControllers[it].user
                    val session = user?.session
                    MatchUserStats(
                        clientType = session?.getSystemProperty(SFSConstants.SESSION_CLIENT_TYPE) as? String? ?: "",
                        sessionType = when (session?.type) {
                            SessionType.DEFAULT -> "default"
                            SessionType.BLUEBOX -> "bluebox"
                            SessionType.VOID -> "void"
                            SessionType.WEBSOCKET -> "websocket"
                            else -> "null"
                        },
                        ip = user?.ipAddress ?: "",
                        country = user?.country?.name ?: "",
                        countryIsoCode = user?.country?.isoCode ?: "",
                        isUdpEnabled = session?.isUdpEnabled ?: false,
                        isEncrypted = session?.isEncrypted ?: false,
                        latency = _networkManager.latencies[it],
                        timeDelta = _networkManager.timeDeltas[it],
                        lossRate = _networkManager.lossRates[it],
                    )
                }
            )
        }

    init {
        _logger.log("[MatchController:init]")
        _heroListener = object : IHeroListener {
            override fun onDamaged(hero: IHero, amount: Int, source: HeroDamageSource) {
                _logger.log("[HeroListener:onDamaged] slot=${hero.slot} amount=$amount source=${source.name}")
            }

            override fun onHealthChanged(hero: IHero, amount: Int, oldAmount: Int) = Unit

            override fun onItemChanged(hero: IHero, item: HeroItem, amount: Int, oldAmount: Int) {
                _logger.log("[HeroListener:onItemChanged] slot=${hero.slot} item=$item amount=$amount oldAmount=$oldAmount")
            }

            override fun onEffectBegan(hero: IHero, effect: HeroEffect, reason: HeroEffectReason, duration: Int) {
                _logger.log("[HeroListener:onEffectBegan] slot=${hero.slot} effect=$effect reason=$reason")
            }

            override fun onEffectEnded(hero: IHero, effect: HeroEffect, reason: HeroEffectReason) {
                _logger.log("[HeroListener:onEffectEnded] slot=${hero.slot} effect=$effect reason=$reason")
            }

            override fun onMoved(hero: IHero, x: Float, y: Float) = Unit
        }
        _bombListener = object : IBombListener {
            override fun onAdded(bomb: IBomb, reason: BombReason) {
                _logger.log("[BombListener:onAdded] slot=${bomb.slot} id=${bomb.id} x=${bomb.x} y=${bomb.y} reason=$reason")
            }

            override fun onRemoved(bomb: IBomb, reason: BombReason) {
                _logger.log("[BombListener:onRemoved] slot=${bomb.slot} id=${bomb.id} x=${bomb.x} y=${bomb.y} reason=$reason")
            }

            override fun onExploded(bomb: IBomb, ranges: Map<Direction, Int>) {
                _logger.log("[BombListener:onExploded] slot=${bomb.slot} id=${bomb.id} x=${bomb.x} y=${bomb.y} ranges=${ranges[Direction.Left]} ${ranges[Direction.Right]} ${ranges[Direction.Up]} ${ranges[Direction.Down]}")
            }

            override fun onDamaged(x: Int, y: Int, amount: Int) = Unit
        }
        _mapListener = object : IMapListener {
            override fun onAdded(block: IBlock, reason: BlockReason) {
                _logger.log("[MapListener:onAdded] x=${block.x} y=${block.y} type=${block.type} reason=$reason")
            }

            override fun onRemoved(block: IBlock, reason: BlockReason) {
                _logger.log("[MapListener:onRemoved] x=${block.x} y=${block.y} type=${block.type} reason=$reason")
            }
        }
    }

    override fun initialize() {
        _scheduler.schedule(::step.name, 0, _stepDuration) {
            step(_stepDuration)
        }
        // Schedule a dedicated thread for network manager.
        _scheduler.schedule(::stepNetwork.name, 0, 300) {
            stepNetwork(300)
        }
        // Ready phase.
        startReady()
    }

    private fun checkStatus(status: MatchStatus): Boolean {
        return synchronized(_statusLocker) {
            _matchData.status == status
        }
    }

    override fun joinRoom(user: User) {
        _logger.log("[MatchController:onJoinRoom] user=$user isPlayer=${user.isPlayer}")
        // FIXME: check observer can join or not.
        val endAction = {
            if (user.isPlayer) {
                // Participant.
                synchronized(_participantLocker) {
                    val info = user.getJoinPVPMatchInfo()
                    _joinedSlots.add(info.slot)
                    _participantControllers[info.slot].join(user)
                }
            } else {
                // Observer.
                synchronized(_observerLocker) {
                    // Find an empty slot.
                    val controller = _observerControllers.firstOrNull { it.user == null } ?: return@synchronized
                    controller.join(user)
                    ++_matchData.observerCount
                }
            }
        }
        if (checkStatus(MatchStatus.Ready)) {
            _messageBridge.startReady(listOf(user))
            endAction()
        }
        if (checkStatus(MatchStatus.Started)) {
            // FIXME: client doesnt support consecutive events.
            _scheduler.scheduleOnce("send_start_$user", 1500) {
                sendStartRound(false, listOf(user))
            }
            _scheduler.scheduleOnce("send_observe_data_$user", 3000) {
                synchronized(_stepLocker) {
                    val changeData = _stateManager.accumulativeChangeData ?: return@synchronized
                    val data = MatchObserveData(
                        id = -1,
                        timestamp = _timeManager.timestamp,
                        matchId = _matchInfo.id,
                        heroDelta = changeData.hero,
                        bombDelta = changeData.bomb,
                        blockDelta = changeData.block,
                    )
                    sendChangeData(data, false, listOf(user))
                }
                endAction()
            }
        }
        if (checkStatus(MatchStatus.Finished)) {
            _scheduler.scheduleOnce("send_finished_$user", 1500) {
                val data = MatchFinishData(_matchData)
                _messageBridge.finishRound(data, listOf(user))
                endAction()
            }
        }
        if (checkStatus(MatchStatus.MatchFinished)) {
            _scheduler.scheduleOnce("send_match_finished_$user", 1500) {
                _messageBridge.finishMatch(_matchResult, listOf(user))
                endAction()
            }
        }
    }

    override fun leaveRoom(user: User) {
        _logger.log("[MatchController:onLeaveRoom] user=$user isPlayer=${user.isPlayer}")
        if (user.isPlayer) {
            // Participant.
            synchronized(_participantLocker) {
                val info = user.getJoinPVPMatchInfo()
                _participantControllers[info.slot].leave()
            }
        } else {
            // Observer.
            synchronized(_observerLocker) {
                val controller = _observerControllers.firstOrNull { it.user == user } ?: return@synchronized
                controller.leave()
                --_matchData.observerCount
            }
        }
    }

    private fun startReady() {
        _logger.log("[MatchController:startReady]")
        _scheduler.scheduleOnce(::forceReady.name, _readyTimeOutDuration) {
            forceReady()
        }
        synchronized(_statusLocker) {
            _matchData.status = MatchStatus.Ready
            _matchData.readyStartTimestamp = _timeManager.timestamp
        }
        _participantControllers.forEach {
            it.reset()
        }
        _messageBridge.startReady(activeUsers)
    }

    private fun finishReady() {
        _logger.log("[MatchController:finishReady]")
        require(checkStatus(MatchStatus.Ready)) {
            "Invalid match status, expected ${MatchStatus.Ready}, found ${_matchData.status}"
        }
        _scheduler.clear(::forceReady.name)
        _messageBridge.finishReady(activeUsers)
        startRound()
    }

    private fun forceReady() {
        _logger.log("[MatchController:forceReady]")
        _participantControllers.forEachIndexed { index, _ -> ready(index) }
    }

    override fun ready(user: User) {
        require(user.isPlayer) { "User is not a participant" }
        val info = user.getJoinPVPMatchInfo()
        ready(info.slot)
    }

    private fun ready(slot: Int) {
        _logger.log("[MatchController:ready] slot=$slot")
        require(checkStatus(MatchStatus.Ready)) {
            "Invalid match status, expected ${MatchStatus.Ready}, found ${_matchData.status}"
        }
        synchronized(_readyLocker) {
            val item = _participantControllers[slot]
            if (item.isReady) {
                // Already ready.
                return
            }
            item.ready()
        }
        // Send events.
        val data = MatchReadyData(_matchInfo.id, slot)
        _messageBridge.ready(data, activeUsers)
    }

    private fun createMatch(random: IRandom): MatchManagerInfo {
        val matchTimeManager = StepTimeManager()
        val heroConfig = object : IHeroConfig {
            override val explodeDuration = 3000
            override val shieldedDuration = 8000
            override val invincibleDuration = 3000
            override val imprisonedDuration = 5000
            override val skullEffectDuration = 10000
        }
        val mapInfo = _mapGenerator.generate()
        val match = Match(
            controllers = _participantControllers,
            teamInfo = _matchInfo.team,
            heroInfo = _matchInfo.info.map { it.hero },
            mapInfo = mapInfo,
            heroConfig = heroConfig,
            initialState = MatchState(
                heroState = HeroManagerState(
                    _matchInfo.info.withIndex().associate {
                        it.index to HeroState(
                            isAlive = true,
                            x = mapInfo.startingPositions[it.index].first + 0.5f, // Convert tile position to item position.
                            y = mapInfo.startingPositions[it.index].second + 0.5f,
                            direction = Direction.Down,
                            health = it.value.hero.health,
                            damageSource = HeroDamageSource.Null,
                            items = emptyMap(),
                            effects = emptyMap(),
                        )
                    },
                ),
                bombState = BombManagerState(0, emptyMap()),
                mapState = MapManagerState.create(mapInfo, true),
            ),
            _logger = _logger,
            _timeManager = matchTimeManager,
            random = random,
            heroListener = _heroListener,
            bombListener = _bombListener,
            mapListener = _mapListener,
        )
        val blockListener = object : IFallingBlockManagerListener {
            override fun onBlockDidFall(x: Int, y: Int) {
                try {
                    // Remove existing bomb.
                    val bomb = match.bombManager.getBomb(x, y)
                    @Suppress("IfThenToSafeAccess")
                    if (bomb != null) {
                        bomb.kill(BombReason.Removed)
                    }
                    // Remove existing block.
                    val block = match.mapManager.getBlock(x, y)
                    @Suppress("IfThenToSafeAccess")
                    if (block != null) {
                        block.kill(BlockReason.Removed)
                    }
                    match.mapManager.addBlock(
                        Block.createHardBlock(
                            x,
                            y,
                            BlockReason.Falling,
                            _logger,
                            match.mapManager
                        )
                    )
                } catch (ex: Exception) {
                    // Block exist.
                }
                match.heroManager.damageFallingBlock(x, y)
            }

            override fun onBuffered(blocks: List<IFallingBlockInfo>) {
                _logger.log("[FallingBlockManagerListener:onBuffered] blocks=${blocks.size}")
                val data = FallingBlockData(_matchInfo.id, blocks)
                _messageBridge.bufferFallingBlocks(data, activeUsers)
            }
        }
        val fallingBlockManager = FallingBlockManager(mapInfo.fallingBlocks, _logger, blockListener)
        val reportManager = DefaultReportManager(_logger, _matchData, _matchInfo.info)
        val stateManager = DefaultStateManager(_logger, match)
        val dataFactory = ObserveDataFactory(_matchInfo)
        val commandManager =
            DefaultCommandManager(_logger, match, _matchData, _packetManager, stateManager, dataFactory)
        val updater = DefaultUpdater(_logger, listOf(fallingBlockManager, match))
        return MatchManagerInfo(
            match = match,
            mapInfo = mapInfo,
            timeManager = matchTimeManager,
            stateManager = stateManager,
            dataFactory = dataFactory,
            commandManager = commandManager,
            reportManager = reportManager,
            updater = updater,
        )
    }

    private fun startRound() {
        _logger.log("[MatchController:startRound]")
        synchronized(_statusLocker) {
            require(_matchData.status == MatchStatus.Ready) {
                "Invalid match status, expected ${MatchStatus.Ready}, found ${_matchData.status}"
            }
            _matchData.status = MatchStatus.Started
            _matchData.roundStartTimestamp = _timeManager.timestamp
        }

        val info = createMatch(_random)

        // Update variables.
        _match = info.match
        _mapInfo = info.mapInfo
        _matchTimeManager = info.timeManager
        _stateManager = info.stateManager
        _dataFactory = info.dataFactory
        _commandManager = info.commandManager
        _reportManager = info.reportManager
        _updater = info.updater

        // Send events.
        sendStartRound(true, activeUsers)
    }

    private fun sendStartRound(report: Boolean, users: List<User>) {
        val data = MatchStartData(_matchData, _mapInfo)
        if (report) {
            _reportManager.start(data)
        }
        _messageBridge.startRound(data, users)
    }

    private fun finishRound(isDraw: Boolean, winningTeam: Int) {
        _logger.log("[MatchController:finishRound] isDraw=$isDraw winningTeam=$winningTeam")
        synchronized(_statusLocker) {
            require(_matchData.status == MatchStatus.Started) {
                "Invalid match status, expected ${MatchStatus.Started}, found ${_matchData.status}"
            }
            _matchData.status = MatchStatus.Finished
            ++_matchData.round
        }

        // Result info.
        val resultInfo = generateRoundResult(isDraw, winningTeam)
        _matchData.results.add(resultInfo)

        // Send events.
        val data = MatchFinishData(_matchData)
        _messageBridge.finishRound(data, activeUsers)
    }

    override fun quit(user: User) {
        require(user.isPlayer) { "User is not a participant" }
        val info = user.getJoinPVPMatchInfo()
        quit(info.slot)
    }

    private fun quit(slot: Int) {
        _logger.log("[MatchController:quit] slot=$slot")
        _participantControllers[slot].quit()
    }

    /**
     * Checks whether the game can finish.
     */
    private fun checkRoundResult(): ResultInfo {
        require(checkStatus(MatchStatus.Started)) {
            "Invalid match status, expected ${MatchStatus.Started}, found ${_matchData.status}"
        }

        // Check states.
        // - true: OK.
        // - false: dead or quited.
        val userStates = synchronized(_stepLocker) {
            _participantControllers.mapIndexed { index, it ->
                val state = _match.heroManager.getHero(index).state
                state.isAlive && !it.isQuited
            }
        }
        // Team states.
        // - true: any user is alive.
        // - false: all users are dead.
        val teamStates = _matchInfo.team.map { team ->
            team.slots.any { userStates[it] }
        }
        val isFinished: Boolean
        val isDraw: Boolean
        val winningTeam: Int
        when (teamStates.count { it } /* Count alive teams */) {
            0 -> {
                // No one alive.
                isFinished = true
                isDraw = true
                winningTeam = -1
            }

            1 -> {
                // Exactly 1 alive user.
                isFinished = true
                isDraw = false
                winningTeam = teamStates.indexOf(true)
            }

            else -> {
                isFinished = false
                isDraw = false
                winningTeam = -1
            }
        }
        return ResultInfo(
            isFinished,
            isDraw,
            winningTeam,
        )
    }

    private fun checkMatchResult(): ResultInfo {
        val teamScores = _matchInfo.team.indices.map { 0 }.toMutableList()
        _matchData.results.forEach {
            if (it.isDraw) {
                // Ignored.
            } else {
                ++teamScores[it.winningTeam]
            }
        }
        val bestScores = teamScores.sortedDescending()
        val maxScore = bestScores[0]
        val isFinished: Boolean
        val isDraw: Boolean
        val winningTeam: Int
        val rule = _matchInfo.rule
        val remainingRound = rule.round - _matchData.round
        if (remainingRound > 0) {
            isDraw = false
            if (bestScores[1] + remainingRound < maxScore) {
                // 2nd-best team have no chance to win.
                isFinished = true
                winningTeam = teamScores.indexOf(maxScore)
            } else {
                isFinished = false
                winningTeam = -1
            }
        } else {
            // Last round or bonus rounds.
            when (teamScores.count { it == maxScore }) {
                1 -> {
                    // Exactly 1 team with max score.
                    isFinished = true
                    isDraw = false
                    winningTeam = teamScores.indexOf(maxScore)
                }

                else -> {
                    if (rule.canDraw) {
                        isFinished = true
                        isDraw = true
                    } else {
                        isFinished = false
                        isDraw = false
                    }
                    winningTeam = -1
                }
            }
        }
        return ResultInfo(
            isFinished,
            isDraw,
            winningTeam,
        )
    }

    private fun generateRoundResult(isDraw: Boolean, winningTeam: Int): IMatchResultInfo {
        return MatchResultInfo(
            isDraw = isDraw,
            winningTeam = winningTeam,
            scores = List(_matchInfo.team.size) { if (it == winningTeam) 1 else 0 },
            duration = (_timeManager.timestamp - _matchData.roundStartTimestamp).toInt(),
            startTimestamp = _matchData.roundStartTimestamp,
            info = _participantControllers.mapIndexed { index, it ->
                // FIXME: synchronize hero.
                val hero = _match.heroManager.getHero(index)
                val rewards = mutableMapOf<Int, Float>()
                hero.items.forEach {
                    when (it.key) {
                        HeroItem.Gold -> rewards[BLOCK_REWARD_TYPE.GOLD.value] = it.value.toFloat()
                        HeroItem.BronzeChest -> rewards[BLOCK_REWARD_TYPE.BRONZE_CHEST.value] = it.value.toFloat()
                        HeroItem.SilverChest -> rewards[BLOCK_REWARD_TYPE.SILVER_CHEST.value] = it.value.toFloat()
                        HeroItem.GoldChest -> rewards[BLOCK_REWARD_TYPE.GOLD_CHEST.value] = it.value.toFloat()
                        HeroItem.PlatinumChest -> rewards[BLOCK_REWARD_TYPE.PLATINUM_CHEST.value] = it.value.toFloat()
                        else -> {
                            // No-op
                        }
                    }
                }
                MatchResultUserInfo(
                    serverId = it.info.serverId,
                    isTest = it.info.isTest,
                    isBot = it.info.isBot,
                    teamId = _matchInfo.team.indexOfFirst { it.slots.contains(index) },
                    userId = it.info.userId,
                    username = it.info.username,
                    matchCount = it.info.matchCount,
                    winMatchCount = it.info.winMatchCount,
                    point = it.info.point,
                    boosters = it.info.boosters,
                    usedBoosters = it.usedBoosters,
                    quit = it.isQuited,
                    hero = MatchResultHeroInfo(
                        id = it.info.hero.id,
                        damageSource = hero.damageSource.ordinal,
                        rewards = rewards,
                        collectedItems = hero.collectedItems,
                    )
                )
            }
        )
    }

    private fun generateMatchResult(isDraw: Boolean, winningTeam: Int): IMatchResultInfo {
        return MatchResultInfo(
            isDraw = isDraw,
            winningTeam = winningTeam,
            scores = List(_matchInfo.team.size) { teamId ->
                _matchData.results
                    .map { result -> result.winningTeam }
                    .count { it == teamId }
            },
            duration = _matchData.results.sumOf { it.duration },
            startTimestamp = _matchData.results[0].startTimestamp,
            info = _participantControllers.mapIndexed { index, it ->
                MatchResultUserInfo(
                    serverId = it.info.serverId,
                    isTest = it.info.isTest,
                    isBot = it.info.isBot,
                    teamId = _matchInfo.team.indexOfFirst { it.slots.contains(index) },
                    userId = it.info.userId,
                    username = it.info.username,
                    matchCount = it.info.matchCount,
                    winMatchCount = it.info.winMatchCount,
                    point = it.info.point,
                    boosters = it.info.boosters,
                    usedBoosters = it.usedBoosters,
                    quit = it.isQuited,
                    hero = MatchResultHeroInfo(
                        id = it.info.hero.id,
                        damageSource = _matchData.results[0].info[index].hero.damageSource,
                        rewards = _matchData.results
                            .map { it.info[index].hero.rewards }
                            .flatMap { it.entries }
                            .groupBy({ it.key }, { it.value })
                            .mapValues { (_, values) -> values.sum() },
                        collectedItems = _matchData.results
                            .map { it.info[index].hero.collectedItems }
                            .flatten()
                    )
                )
            }
        )
    }

    private fun createResultInfo(resultInfo: IMatchResultInfo): IPvpResultInfo {
        val points = resultInfo.info.map { it.point }
        val info = PvpResultInfo(
            id = matchInfo.id,
            serverId = matchInfo.serverId,
            timestamp = matchInfo.timestamp,
            mode = matchInfo.mode,
            isDraw = resultInfo.isDraw,
            winningTeam = resultInfo.winningTeam,
            scores = resultInfo.scores,
            duration = resultInfo.duration,
            rule = matchInfo.rule,
            team = matchInfo.team,
            info = resultInfo.info.mapIndexed { index, userInfo ->
                val isWinner = resultInfo.winningTeam == userInfo.teamId && !resultInfo.isDraw
                val rankResult = _rankManager.calculate(
                    resultInfo.isDraw,
                    isWinner,
                    index,
                    userInfo.boosters,
                    points,
                )
                val usedBoosters = userInfo.usedBoosters.toMutableMap()
                rankResult.usedBoosters.forEach {
                    usedBoosters.merge(it, 1, Int::plus)
                }
                val rewards = if (isWinner) userInfo.hero.rewards else emptyMap()
                PvpResultUserInfo(
                    serverId = userInfo.serverId,
                    isBot = userInfo.isBot,
                    teamId = userInfo.teamId,
                    userId = userInfo.userId,
                    username = userInfo.username,
                    rank = rankResult.rank,
                    point = rankResult.point,
                    matchCount = userInfo.matchCount + 1,
                    winMatchCount = userInfo.winMatchCount + (if (isWinner) 1 else 0),
                    deltaPoint = rankResult.deltaPoint,
                    usedBoosters = usedBoosters,
                    quit = userInfo.quit,
                    heroId = userInfo.hero.id,
                    damageSource = userInfo.hero.damageSource,
                    rewards = rewards,
                    collectedItems = userInfo.hero.collectedItems,
                )
            }
        )
        return info
    }

    /**
     * Finishes the current match.
     */
    private fun finishMatch(isDraw: Boolean, winningTeam: Int): IPvpResultInfo {
        _logger.log("[MatchController:finishMatch] isDraw=$isDraw winningTeam=$winningTeam")
        synchronized(_statusLocker) {
            require(_matchData.status == MatchStatus.Finished) {
                "Invalid match status, expected ${MatchStatus.Finished}, found ${_matchData.status}"
            }
            _matchData.status = MatchStatus.MatchFinished
        }
        // Result info.
        val matchResultInfo = generateMatchResult(isDraw, winningTeam)
        _reportManager.finish(matchResultInfo)
        val historyInfo = _reportManager.info
        val resultInfo = createResultInfo(matchResultInfo)
        _messageBridge.finishMatch(resultInfo, activeUsers)
        val stats = matchStats // Store the current stats.
        try {
            // Update main server.
            _matchManager.finish(resultInfo, historyInfo, stats)
        } catch (e: Exception) {
            _logger.log("[MatchController:finishMatch] exception=${e.message} \n ${e.stackTraceToString()}")
        }
        return resultInfo
    }

    override fun ping(user: User, timestamp: Long, requestId: Int) {
        if (user.isPlayer) {
            // Participant.
            val info = user.getJoinPVPMatchInfo()
            _networkManager.pong(_participantControllers[info.slot], timestamp, requestId)
        } else {
            // Observer.
            synchronized(_observerLocker) {
                val controller = _observerControllers.firstOrNull { it.user == user } ?: return@synchronized
                _networkManager.pong(controller, timestamp, requestId)
            }
        }
    }

    override suspend fun moveHero(user: User, timestamp: Long, x: Float, y: Float): IMoveHeroData {
        require(user.isPlayer) { "User is not a participant" }
        require(checkStatus(MatchStatus.Started)) {
            "Invalid match status: ${_matchData.status}"
        }
        val info = user.getJoinPVPMatchInfo()
        val slot = info.slot
        val serverTimestamp = timestamp + _networkManager.timeDeltas[slot]
        val matchTimestamp = (serverTimestamp - _matchData.roundStartTimestamp).toInt()
        return _commandManager.moveHero(slot, matchTimestamp, x, y)
    }

    override suspend fun plantBomb(user: User, timestamp: Long): IPlantBombData {
        require(user.isPlayer) { "User is not a participant" }
        require(checkStatus(MatchStatus.Started)) {
            "Invalid match status: ${_matchData.status}"
        }
        val info = user.getJoinPVPMatchInfo()
        val slot = info.slot
        val serverTimestamp = timestamp + _networkManager.timeDeltas[slot]
        val matchTimestamp = (serverTimestamp - _matchData.roundStartTimestamp).toInt()
        return _commandManager.plantBomb(slot, matchTimestamp)
    }

    override suspend fun throwBomb(user: User, timestamp: Long) {
        require(user.isPlayer) { "User is not a participant" }
        require(checkStatus(MatchStatus.Started)) {
            "Invalid match status: ${_matchData.status}"
        }
        val info = user.getJoinPVPMatchInfo()
        val slot = info.slot
        val serverTimestamp = timestamp + _networkManager.timeDeltas[slot]
        val matchTimestamp = (serverTimestamp - _matchData.roundStartTimestamp).toInt()
        return _commandManager.throwBomb(slot, matchTimestamp)
    }

    override suspend fun useBooster(user: User, timestamp: Long, itemId: Int) {
        require(user.isPlayer) { "User is not a participant" }
        require(checkStatus(MatchStatus.Started)) {
            "Invalid match status: ${_matchData.status}"
        }
        val info = user.getJoinPVPMatchInfo()
        val slot = info.slot
        val serverTimestamp = timestamp + _networkManager.timeDeltas[slot]
        val matchTimestamp = (serverTimestamp - _matchData.roundStartTimestamp).toInt()
        _commandManager.useBooster(slot, matchTimestamp, Booster.fromValue(itemId))
    }

    override fun useEmoji(user: User, itemId: Int) {
        require(user.isPlayer) { "User is not a participant" }
        require(checkStatus(MatchStatus.Started)) {
            "Invalid match status: ${_matchData.status}"
        }
        val info = user.getJoinPVPMatchInfo()
        val slot = info.slot
        require(info.info[slot].hero.skinChests[ItemType.EMOJI.value]?.contains(itemId) ?: false) {
            "Invalid emoji: $itemId"
        }
        val data = UseEmojiData(_matchInfo.id, slot, itemId)
        _messageBridge.useEmoji(data, activeUsers)
    }

    private fun sendChangeData(
        data: IMatchObserveData,
        report: Boolean,
        users: List<User>,
    ) {
        require(
            data.heroDelta.isNotEmpty() ||
                data.bombDelta.isNotEmpty() ||
                data.blockDelta.isNotEmpty()
        ) {
            "Empty change data"
        }
        if (report) {
            _reportManager.observe(data)
        }
        _packetManager.add {
            _messageBridge.changeState(data, users)
        }
    }

    private fun stepNetwork(delta: Int) {
        _networkManager.step(delta)
    }

    private fun step(delta: Int) {
        if (checkStatus(MatchStatus.Ready)) {
            val joined = synchronized(_participantLocker) { _joinedSlots.size == _participantControllers.size }
            if (joined) {
                _networkStepDuration += delta
            }
            if (_networkStepDuration >= 1000) {
                val readied = synchronized(_readyLocker) { _participantControllers.all { it.isReady } }
                if (readied) {
                    finishReady()
                }
            }
            return
        }
        if (checkStatus(MatchStatus.Started)) {
            synchronized(_stepLocker) {
                _updater.step(delta)
                val dataList = _commandManager.processCommands()
                dataList.forEach {
                    sendChangeData(it, true, activeUsers)
                }
                val stateDelta = _stateManager.processState()
                if (stateDelta != null) {
                    val observeData = _dataFactory.generate(_timeManager.timestamp, stateDelta)
                    sendChangeData(observeData, true, activeUsers)
                }
                _matchTimeManager.step(delta)
            }
            _packetManager.flush()

            // Check finish.
            val result = checkRoundResult()
            if (result.isFinished) {
                finishRound(result.isDraw, result.winningTeam)
            }
            return
        }
        if (checkStatus(MatchStatus.Finished)) {
            val result = checkMatchResult()
            if (result.isFinished) {
                _matchResult = finishMatch(result.isDraw, result.winningTeam)
            } else {
                startReady()
            }
        }
    }
}