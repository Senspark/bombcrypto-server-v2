package com.senspark.game.manager.blockMap

import com.senspark.common.service.IScheduler
import com.senspark.common.utils.LazyMutable
import com.senspark.game.controller.MapData
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.manager.block.IBlockConfigManager
import com.senspark.game.data.manager.block.IBlockDropByDayManager
import com.senspark.game.data.manager.block.IBlockRewardDataManager
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.user.BlockMap
import com.senspark.game.data.model.user.RewardDetail
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.declare.EnumConstants.MODE
import com.senspark.game.declare.EnumConstants.SAVE
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.GameConstants
import com.senspark.game.declare.GameConstants.MAP_MAX_COL
import com.senspark.game.declare.GameConstants.MAP_MAX_ROW
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.exception.CustomException
import com.senspark.game.extension.coroutines.ICoroutineScope
import com.senspark.game.manager.IEnvManager
import com.senspark.game.manager.blockMap.mapservice.IMapServiceClient
import com.senspark.game.manager.blockMap.mapservice.IMapTreasureEventRouter
import com.senspark.game.manager.blockMap.mapservice.MsAutoHeroDto
import com.senspark.game.manager.blockMap.mapservice.MsAutoHeroesRequest
import com.senspark.game.manager.blockMap.mapservice.MsAutoSnapshotDto
import com.senspark.game.manager.blockMap.mapservice.MsAutoStartRequest
import com.senspark.game.manager.blockMap.mapservice.MapServiceSessionNotFoundException
import com.senspark.game.manager.blockMap.mapservice.MsBlockDto
import com.senspark.game.manager.blockMap.mapservice.MsBlockHitDto
import com.senspark.game.manager.blockMap.mapservice.MsHeroSnapshotDto
import com.senspark.game.manager.blockMap.mapservice.MsMapInitRequest
import com.senspark.game.manager.blockMap.mapservice.MsMapReplaceRequest
import com.senspark.game.manager.blockMap.mapservice.MsRewardConfigDto
import com.senspark.game.manager.blockMap.mapservice.MsRewardEntryDto
import com.senspark.game.manager.blockMap.mapservice.MsTreasureEventBatch
import com.senspark.game.manager.blockMap.mapservice.MsTreasureEventDto
import com.senspark.game.manager.blockMap.mapservice.MsTreasureEventType
import com.senspark.game.manager.blockReward.IUserBlockRewardManager
import com.senspark.game.manager.hero.IUserHeroFiManager
import com.senspark.game.manager.stake.IHeroStakeManager
import com.senspark.game.manager.treasureHuntV2.ITreasureHuntV2Manager
import com.senspark.game.manager.treasureHuntV2.UserId
import com.senspark.game.pvp.HandlerCommand
import com.senspark.game.utils.Utils
import com.senspark.lib.data.manager.IGameConfigManager
import com.senspark.lib.utils.Util
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.EnumMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

// Treasure-mode map manager: MapService plays the game, results come back via
// AP_MAP_TREASURE_EVENT_CHANNEL into [onTreasureEvents].
class UserBlockMapManagerV2(
    private val _mediator: UserControllerMediator,
    private val _blockRewardManager: IUserBlockRewardManager,
    private val _heroFiManager: IUserHeroFiManager,
    private val _mapServiceClient: IMapServiceClient,
) : IUserBlockMapManagerV2 {

    private val _blockConfigManager = _mediator.services.get<IBlockConfigManager>()
    private val _dataAccessManager = _mediator.services.get<IDataAccessManager>()
    private val _blockDropRateManager = _mediator.services.get<IBlockDropByDayManager>()
    private val _blockRewardDataManager = _mediator.services.get<IBlockRewardDataManager>()
    private val _gameConfigManager = _mediator.services.get<IGameConfigManager>()
    private val _coroutineScope = _mediator.services.get<ICoroutineScope>()

    private val _treasureHuntV2Manager = _mediator.svServices.get<ITreasureHuntV2Manager>()
    private val _heroStakeManager = _mediator.svServices.get<IHeroStakeManager>()

    // Map DB code duplicated from UserBlockMapManagerImpl on purpose, to keep Impl unchanged.
    private var _mapData: MapData by LazyMutable { initMapData() }
    private val _fixedMode = MODE.PVE_V2
    override val locker = Any()

    private val _sessionKey get() = "${_mediator.userId}-${_mediator.dataType}-$_fixedMode"

    @Volatile
    private var _sessionEverInitialized = false

    // ================== BLOCK MAP ==================

    private fun initMapData(): MapData {
        return _dataAccessManager.gameDataAccess.loadSingleMapData(_mediator.userId, _mediator.dataType, _fixedMode)
            ?: initFirstMap()
    }

    private fun initFirstMap(): MapData {
        val mapData = createRandomMap(_fixedMode)
        if (!mapData.containBlockHPLeft()) {
            throw CustomException("Server Error x043 ", ErrorCode.CREATE_MAP_FAIL)
        }
        _dataAccessManager.gameDataAccess.insertMapData(
            _mediator.userId,
            mapData.castBlocksToJsonArray(),
            _mediator.dataType,
            mapData.createdDate,
            mapData.tileset,
            mapData.mode
        )
        return mapData
    }

    override fun saveMap(userId: Int, needSave: MutableMap<SAVE, Boolean>) {
        val saveKey = SAVE.MAP
        if (!needSave[saveKey]!!) {
            return
        }
        needSave[saveKey] = false
        try {
            _dataAccessManager.gameDataAccess.updateSingleMapData(userId, _mapData, _mediator.dataType)
        } catch (e: Exception) {
            _mediator.logger.error("Error save map data: ${e.message}")
        }
    }

    private fun createRandomMap(pveMode: MODE): MapData {
        val col = MAP_MAX_COL
        val row = MAP_MAX_ROW
        val map = Array(col) { IntArray(row) }
        val blockDropRate = _blockDropRateManager.getBlockDropRate(_mediator.dataType, 1)
        val mapdata = MapData()
        run {
            var i = 1
            while (i < col) {
                var j = 1
                while (j < row) {
                    map[i][j] = 1
                    j += 2
                }
                i += 2
            }
        }
        val density = _gameConfigManager.blockDensity

        val randBlocks = mutableListOf<BlockMap>()
        for (i in 0 until col) {
            for (j in 0 until row) {
                if (map[i][j] == 0) {
                    val rand = Util.randFloat(0f, 1f)
                    if (rand < density) {
                        randBlocks.add(createBlockMap(i, j, blockDropRate))
                    }
                }
            }
        }
        mapdata.addBlocks(randBlocks)
        mapdata.reposition()
        val titleset: Int = if (_mediator.dataType != DataType.TON && _mediator.dataType != DataType.SOL) {
            Utils.randInt(0, _gameConfigManager.maxTitleset)
        } else {
            Utils.randInt(0, _gameConfigManager.maxTitleset * 2 + 1)
        }
        mapdata.tileset = titleset
        mapdata.mode = pveMode
        return mapdata
    }

    private fun createBlockMap(i: Int, j: Int, blockDroprate: List<Int>): BlockMap {
        var sum = 0
        for (k in blockDroprate.indices) {
            sum += blockDroprate[k]
        }
        val rand = Util.randInt(1, sum)
        var total = 0
        var blockType = 1
        val len = blockDroprate.size
        for (k in 0 until len) {
            total += blockDroprate[k]
            if (rand <= total) {
                blockType = k + 1
                break
            }
        }
        val block = _blockConfigManager.getConfig(_mediator.dataType, blockType)
        val bm = BlockMap()
        bm.i = i
        bm.j = j
        bm.type = blockType
        bm.hp = block.hp
        bm.maxHp = block.hp
        return bm
    }

    // MapService push failure is swallowed: client still needs PVE_NEW_MAP, next call re-inits the session.
    // Treasure mode announces the map through its own NEW_MAP event instead of PVE_NEW_MAP.
    private fun createNewMap(pushLegacyNewMap: Boolean = true) {
        val mapData = createRandomMap(_fixedMode)
        if (!mapData.containBlockHPLeft()) {
            throw CustomException("Server Error x043 ", ErrorCode.CREATE_MAP_FAIL)
        }
        _mapData = mapData
        _mediator.saveLater(SAVE.MAP)
        try {
            withMapServiceSession {
                _mapServiceClient.replaceMap(
                    _sessionKey,
                    MsMapReplaceRequest(blocks = toDtoBlocks(mapData), tileset = mapData.tileset, mode = _fixedMode.name)
                )
            }
        } catch (e: Exception) {
            _mediator.logger.error("[MAP_SERVICE] failed to push new map for $_sessionKey, will re-init on next access", e)
            _sessionEverInitialized = false
            if (_treasureActive) _treasureNeedsResync = true
        }
        if (pushLegacyNewMap && !_treasureActive) {
            _mediator.sendDataEncryption(SFSCommand.PVE_NEW_MAP, SFSObject(), true)
        }
    }

    @Throws(CustomException::class)
    override fun getBlockMap(): ISFSObject {
        val hasAnyBlock = _mapData.containBlockHPLeft()
        if (!hasAnyBlock) {
            createNewMap()
        } else {
            try {
                ensureSessionInitialized()
            } catch (e: Exception) {
                // Best-effort: map is still viewable if MapService is down.
                _mediator.logger.error("[MAP_SERVICE] ensureSessionInitialized failed for $_sessionKey", e)
            }
        }
        val data: ISFSObject = SFSObject()
        val modeName = _fixedMode.name.lowercase()
        data.putUtfString("${SFSField.Datas}_${modeName}", _mapData.toJson())
        data.putInt("${SFSField.Tileset}_${modeName}", _mapData.tileset)
        return data
    }

    // ================== MAPSERVICE SESSION PLUMBING ==================

    private fun toDtoBlocks(mapData: MapData) = mapData.blocks.map { MsBlockDto(it.i, it.j, it.type, it.hp, it.maxHp) }

    private fun buildInitRequest(): MsMapInitRequest {
        // Keyed "$dataType-$blockType", same as BlockRewardDataManager.
        val rewardSnapshot = _blockRewardDataManager.getRewardOptionsSnapshot(_mediator.dataType)
        val rewardTables = rewardSnapshot.entries.associate { (blockType, options) ->
            "${_mediator.dataType}-$blockType" to options.map { MsRewardEntryDto(it.type.name, it.weight, it.value, it.value) }
        }
        return MsMapInitRequest(
            blocks = toDtoBlocks(_mapData),
            tileset = _mapData.tileset,
            mode = _fixedMode.name,
            rewardConfig = MsRewardConfigDto(
                rewardTables = rewardTables,
                minStakeBcoinTHV1 = _gameConfigManager.minStakeBcoinTHV1,
                minStakeSenTHV1 = _gameConfigManager.minStakeSenTHV1,
                minStakeHeroConfig = _heroStakeManager.minStakeHeroConfig.entries.associate { (k, v) -> k.toString() to v },
            ),
        )
    }

    private fun ensureSessionInitialized() {
        if (_sessionEverInitialized) return
        synchronized(locker) {
            if (_sessionEverInitialized) return
            _mapServiceClient.initSession(_sessionKey, buildInitRequest())
            _sessionEverInitialized = true
        }
    }

    // On session-not-found, re-init from local map and retry once.
    private fun <T> withMapServiceSession(block: () -> T): T {
        ensureSessionInitialized()
        return try {
            block()
        } catch (e: MapServiceSessionNotFoundException) {
            _mediator.logger.log("[MAP_SERVICE] session missing for $_sessionKey, re-initializing")
            _mapServiceClient.initSession(_sessionKey, buildInitRequest())
            block()
        }
    }

    private fun <T> callMapService(block: () -> T): T {
        return try {
            withMapServiceSession(block)
        } catch (e: Exception) {
            _mediator.logger.error("[MAP_SERVICE] call failed for $_sessionKey", e)
            throw CustomException("Map service unavailable", ErrorCode.MAP_SERVICE_ERROR)
        }
    }

    override fun notifySessionEnd() {
        if (_registeredForTreasureEvents) {
            _treasureRouter.unregister(_sessionKey, this)
            _registeredForTreasureEvents = false
        }
        if (_treasureActive) {
            _treasureActive = false
            _scheduler.clear(_treasureKeepaliveKey)
        }
        _scheduler.clear(_treasureFlushKey)
        if (!_sessionEverInitialized) return
        _coroutineScope.scope.launch(Dispatchers.IO) {
            try {
                _mapServiceClient.deleteSession(_sessionKey)
            } catch (e: Exception) {
                _mediator.logger.error("[MAP_SERVICE] failed to delete session $_sessionKey on logout", e)
            }
        }
    }

    private fun buildHeroSnapshot(bbm: Hero) = MsHeroSnapshotDto(
        heroId = bbm.heroId,
        bombRange = bbm.bombRange,
        pierceBlock = bbm.containsAbility(GameConstants.BOMBER_ABILITY.PIERCE_BLOCK),
        damageTreasure = bbm.damageTreasure,
        damageJail = bbm.damageJail,
        totalPower = bbm.totalPower,
        stakeBcoin = bbm.stakeBcoin,
        stakeSen = bbm.stakeSen,
        rarity = bbm.rarity,
        isHeroS = bbm.isHeroS,
        dataType = _mediator.dataType.name,
        isAirdropUser = _mediator.isAirdropUser(),
    )

    // Mirror MapService's authoritative hp so getBlockMap/saveMap stay correct.
    private fun mirrorBlocksHit(blocksHit: List<MsBlockHitDto>) {
        for (hit in blocksHit) {
            val localBlock = _mapData.getBlockMap(hit.i, hit.j) ?: continue
            localBlock.hp = hit.hp
            if (localBlock.hp <= 0) {
                _mapData.removeBlockMap(localBlock)
            }
        }
    }

    // Returns the EXPLODE event payload, or null when the hero is no longer credited.
    private fun applyExplodeToHero(event: MsTreasureEventDto, blocksHit: List<MsBlockHitDto>): ISFSObject? {
        val heroId = event.heroId!!
        val bombNo = event.bombNo!!
        val col = event.i!!
        val row = event.j!!

        val bbm = _heroFiManager.getHero(heroId, _mediator.dataType)
        if (bbm == null || bbm.details.dataType != _mediator.dataType) {
            _mediator.logger.log("[EXPLODE_V2] explodeResult SKIP hero=$heroId bombNo=$bombNo reason=bomberman_null")
            return null
        }
        if (!bbm.isActive) {
            _mediator.logger.log("[EXPLODE_V2] explodeResult SKIP hero=$heroId bombNo=$bombNo reason=active_invalid")
            return null
        }
        if (bbm.stage != GameConstants.BOMBER_STAGE.WORK) {
            _mediator.logger.log("[EXPLODE_V2] explodeResult SKIP hero=$heroId bombNo=$bombNo reason=not_working stage=${bbm.stage}")
            return null
        }

        if (bbm.energy <= 0) {
            val resultData: ISFSObject = SFSObject()
            resultData.putLong(SFSField.ID, bbm.heroId.toLong())
            resultData.putInt("num", bombNo)
            resultData.putInt("i", col)
            resultData.putInt("j", row)
            resultData.putInt(SFSField.Energy, bbm.energy)
            resultData.putSFSArray(SFSField.Blocks, SFSArray())
            resultData.putIntArray("attend_pools", listOf<Int>())
            resultData.putInt(SFSField.HeroType, bbm.type.value)
            return resultData
        }

        val isAirdrop = _mediator.isAirdropUser()

        bbm.subEnergy()

        val blocksResult: ISFSArray = SFSArray()
        val mapReward: MutableMap<BLOCK_REWARD_TYPE, RewardDetail> = EnumMap(BLOCK_REWARD_TYPE::class.java)
        var attendPools = listOf<Int>()
        var isDamTreasure = false

        for (hit in blocksHit) {
            val blockResult: ISFSObject = SFSObject()
            blockResult.putInt("i", hit.i)
            blockResult.putInt("j", hit.j)
            blockResult.putInt(SFSField.HP, hit.hp)

            if (hit.type != GameConstants.BLOCK_TYPE.JAIL) {
                isDamTreasure = true
            }

            if (hit.rewards.isNotEmpty()) {
                val rewards: ISFSArray = SFSArray()
                blockResult.putSFSArray(SFSField.Rewards, rewards)
                for (reward in hit.rewards) {
                    val rw: ISFSObject = SFSObject()
                    rw.putUtfString(SFSField.Type, reward.type)
                    rw.putFloat(SFSField.Value, reward.value)
                    rewards.addSFSObject(rw)

                    val type = BLOCK_REWARD_TYPE.valueOf(reward.type)
                    val rewardDetail = mapReward.getOrPut(type) { RewardDetail(type, _fixedMode, _mediator.dataType, 0f) }
                    rewardDetail.addValue(reward.value)
                }
            }
            blocksResult.addSFSObject(blockResult)

            if (!isAirdrop && hit.hp <= 0 &&
                hit.type != GameConstants.BLOCK_TYPE.JAIL && hit.type != GameConstants.BLOCK_TYPE.NORMAL
            ) {
                attendPools = _treasureHuntV2Manager.addHeroToPool(
                    bbm,
                    UserId(_mediator.userId, _mediator.userName, _mediator.dataType)
                )
            }
        }

        _blockRewardManager.addRewards(mapReward)
        _mediator.saveLater(SAVE.REWARD)

        val resultData: ISFSObject = SFSObject()
        resultData.putLong(SFSField.ID, bbm.heroId.toLong())
        resultData.putInt(SFSField.Energy, bbm.energy)
        resultData.putSFSArray(SFSField.Blocks, blocksResult)
        resultData.putInt("num", bombNo)
        resultData.putInt("i", col)
        resultData.putInt("j", row)

        if (isAirdrop) {
            resultData.putIntArray("attend_pools", listOf<Int>())
            resultData.putInt(SFSField.HeroType, bbm.type.value)
        } else {
            resultData.putIntArray("attend_pools", attendPools)
            resultData.putInt(SFSField.IsDangerous, 0)
            if (bbm.isDangerous) {
                val isAvoid = bbm.isAvoid(isDamTreasure)
                if (!isAvoid) {
                    bbm.killBomberman()
                }
                resultData.putInt(SFSField.Energy, bbm.energy)
                resultData.putInt(SFSField.IsDangerous, if (isAvoid) 2 else 1)
            }
            bbm.subStaminaShield(isDamTreasure)
            resultData.putBool("is_trial", false)
        }

        _mediator.saveLater(SAVE.HERO_STATUS)
        _mediator.logger.log(
            "[EXPLODE_V2] explodeResult OK hero=$heroId bombNo=$bombNo pos=$col,$row energy=${bbm.energy} " +
                "fuseLatencyMs=${System.currentTimeMillis() - (event.plantedAtMs ?: 0)}"
        )
        return resultData
    }

    // ================== Server-driven treasure mode (server/docs/treasure_server_driven.md) ==================
    // MapService plays every working hero; this side credits energy/rewards and pushes TREASURE_EVENTS.

    private val _treasureRouter by lazy { _mediator.services.get<IMapTreasureEventRouter>() }
    private val _scheduler by lazy { _mediator.services.get<IScheduler>() }

    // Serializes start/stop/roster/keepalive calls so they reach MapService in order. Lock order: this, then locker.
    private val _treasureSync = ReentrantLock()

    @Volatile
    private var _treasureActive = false

    @Volatile
    private var _treasureNeedsResync = false

    @Volatile
    private var _registeredForTreasureEvents = false

    // Client's pause state; sent on every auto/start so a resync keeps it.
    @Volatile
    private var _treasurePaused = false

    // What MapService is currently playing, as last sent (guarded by locker).
    private val _treasureRoster = LinkedHashMap<Int, MsAutoHeroDto>()
    private val _treasureKeepaliveKey get() = "treasure-keepalive-$_sessionKey"

    // Events waiting for the next TREASURE_EVENTS push, one push per TREASURE_EVENTS_BATCH_MS (guarded by locker).
    private val _treasureBatchMs by lazy { _mediator.services.get<IEnvManager>().treasureEventsBatchMs }
    private var _treasureOutbox = SFSArray()
    private var _treasureFlushScheduled = false
    private val _treasureFlushKey get() = "treasure-flush-$_sessionKey"

    // Caller holds locker and has added to _treasureOutbox.
    private fun scheduleTreasureFlush() {
        if (_treasureBatchMs <= 0) return flushTreasureEvents()
        if (_treasureFlushScheduled || _treasureOutbox.size() == 0) return
        _treasureFlushScheduled = true
        _scheduler.scheduleOnce(_treasureFlushKey, _treasureBatchMs) {
            synchronized(locker) {
                _treasureFlushScheduled = false
                flushTreasureEvents()
            }
        }
    }

    // Caller holds locker. Sends everything queued, in order, as one push.
    private fun flushTreasureEvents() {
        if (_treasureOutbox.size() == 0) return
        val payload = SFSObject()
        payload.putSFSArray("events", _treasureOutbox)
        _treasureOutbox = SFSArray()
        _mediator.sendDataEncryption(HandlerCommand.TreasureEvents, payload, false)
    }

    override fun startTreasureMode(paused: Boolean): ISFSObject = _treasureSync.withLock {
        _treasurePaused = paused
        synchronized(locker) {
            if (!_mapData.containBlockHPLeft()) createNewMap(pushLegacyNewMap = false)
        }
        _treasureRouter.register(_sessionKey, this)
        _registeredForTreasureEvents = true
        val snapshot = callMapService { autoStart() }
        val response = synchronized(locker) {
            _treasureActive = true
            _treasureNeedsResync = false
            adoptBlocks(snapshot)
            treasureSnapshotPayload(snapshot)
        }
        if (!_scheduler.isScheduled(_treasureKeepaliveKey)) {
            _scheduler.schedule(_treasureKeepaliveKey, TREASURE_KEEPALIVE_MS, TREASURE_KEEPALIVE_MS) {
                // Same lock as start/stop/roster: a client START retry must not interleave with the keepalive's restart.
                _coroutineScope.scope.launch(Dispatchers.IO) { _treasureSync.withLock { treasureKeepalive() } }
            }
        }
        if (snapshot.awaitingNewMap) {
            synchronized(locker) { createNewMap(pushLegacyNewMap = false) }
        }
        _mediator.logger.log("[TREASURE] start heroes=${snapshot.heroes.map { it.heroId }} seq=${snapshot.seq} paused=$paused")
        response
    }

    override fun stopTreasureMode() = _treasureSync.withLock {
        _treasurePaused = false
        if (!_treasureActive) return@withLock
        _treasureActive = false
        _scheduler.clear(_treasureKeepaliveKey)
        synchronized(locker) { _treasureRoster.clear() }
        try {
            _mapServiceClient.autoStop(_sessionKey)
        } catch (e: Exception) {
            _mediator.logger.error("[TREASURE] stop failed for $_sessionKey", e)
        }
    }

    override fun setTreasurePaused(paused: Boolean) = _treasureSync.withLock {
        _treasurePaused = paused
        if (_treasureActive) sendTreasurePause()
    }

    // Caller holds _treasureSync.
    private fun sendTreasurePause() {
        try {
            if (!withMapServiceSession { _mapServiceClient.autoPause(_sessionKey, _treasurePaused) }) {
                restartTreasure("map_service_lost")
            }
        } catch (e: Exception) {
            _mediator.logger.error("[TREASURE] pause=$_treasurePaused failed for $_sessionKey", e)
        }
    }

    override fun syncTreasureHeroes() {
        if (!_treasureActive) return
        _coroutineScope.scope.launch(Dispatchers.IO) {
            _treasureSync.withLock { reconcileTreasureRoster() }
        }
    }

    // Caller holds _treasureSync. Sends MapService only the difference to what it's already playing.
    private fun reconcileTreasureRoster() {
        if (!_treasureActive) return
        if (_treasureNeedsResync) return restartTreasure("resync")
        val (upserts, removals) = synchronized(locker) {
            val desired = desiredTreasureHeroes()
            val upserts = desired.values.filter { _treasureRoster[it.hero.heroId] != it }
            val removals = _treasureRoster.keys.filter { it !in desired }.groupBy { treasureLeaveReason(it) }
            upserts to removals
        }
        if (upserts.isEmpty() && removals.isEmpty()) return
        try {
            val requests = removals.map { (reason, ids) -> MsAutoHeroesRequest(remove = ids, reason = reason) }.toMutableList()
            if (requests.isEmpty()) requests.add(MsAutoHeroesRequest(upsert = upserts)) else requests[0] = requests[0].copy(upsert = upserts)
            for (request in requests) {
                if (!withMapServiceSession { _mapServiceClient.autoHeroes(_sessionKey, request) }) {
                    return restartTreasure("map_service_lost")
                }
            }
            synchronized(locker) {
                removals.values.flatten().forEach { _treasureRoster.remove(it) }
                upserts.forEach { _treasureRoster[it.hero.heroId] = it }
            }
        } catch (e: Exception) {
            _mediator.logger.error("[TREASURE] roster sync failed for $_sessionKey", e)
        }
    }

    // Caller holds _treasureSync.
    private fun treasureKeepalive() {
        if (!_treasureActive) return
        try {
            val alive = _mapServiceClient.autoKeepalive(_sessionKey)
            if (!alive.running) return restartTreasure("map_service_lost")
            // A pause call that failed earlier is retried here.
            if (alive.paused != _treasurePaused) sendTreasurePause()
            reconcileTreasureRoster()
        } catch (e: MapServiceSessionNotFoundException) {
            _sessionEverInitialized = false
            restartTreasure("map_service_lost")
        } catch (e: Exception) {
            _mediator.logger.error("[TREASURE] keepalive failed for $_sessionKey", e)
        }
    }

    // MapService lost or never got our state: start again and hand the client a fresh snapshot (RESYNC).
    private fun restartTreasure(reason: String) {
        try {
            val snapshot = withMapServiceSession { autoStart() }
            synchronized(locker) {
                _treasureNeedsResync = false
                adoptBlocks(snapshot)
                val resync = treasureSnapshotPayload(snapshot)
                resync.putUtfString("type", "RESYNC")
                resync.putUtfString("reason", reason)
                resync.putLong("seq", snapshot.seq)
                resync.putLong("at", snapshot.serverTimeMs)
                // Not delayed, but still behind anything already queued.
                _treasureOutbox.addSFSObject(resync)
                flushTreasureEvents()
                if (snapshot.awaitingNewMap) createNewMap(pushLegacyNewMap = false)
            }
            _mediator.logger.log("[TREASURE] resync reason=$reason seq=${snapshot.seq}")
        } catch (e: Exception) {
            _treasureNeedsResync = true
            _mediator.logger.error("[TREASURE] resync failed for $_sessionKey reason=$reason", e)
        }
    }

    // Updates _treasureRoster to what is being sent; caller holds _treasureSync.
    private fun autoStart(): MsAutoSnapshotDto {
        val heroes = synchronized(locker) {
            desiredTreasureHeroes().also {
                _treasureRoster.clear()
                _treasureRoster.putAll(it)
            }
        }
        return _mapServiceClient.autoStart(
            _sessionKey,
            MsAutoStartRequest(heroes.values.toList(), _gameConfigManager.timeBombExplode.toLong(), TREASURE_MAP_RESET_PAUSE_MS, _treasurePaused),
        )
    }

    // Working heroes of this data type with energy left, same cap as the client's map.
    private fun desiredTreasureHeroes(): Map<Int, MsAutoHeroDto> {
        return _heroFiManager.activeHeroes
            .filter { it.details.dataType == _mediator.dataType && it.stage == GameConstants.BOMBER_STAGE.WORK && it.energy > 0 }
            .sortedBy { it.heroId }
            .take(MAX_TREASURE_HEROES)
            .associateTo(LinkedHashMap()) {
                it.heroId to MsAutoHeroDto(
                    hero = buildHeroSnapshot(it),
                    speed = it.speed,
                    bombCount = it.bombCount,
                    blockPass = it.containsAbility(GameConstants.BOMBER_ABILITY.BLOCK_PASS),
                )
            }
    }

    private fun treasureLeaveReason(heroId: Int): String {
        val bbm = _heroFiManager.getHero(heroId, _mediator.dataType) ?: return "removed"
        return when {
            !bbm.isActive -> "inactive"
            bbm.stage != GameConstants.BOMBER_STAGE.WORK -> "not_working"
            bbm.energy <= 0 -> "no_energy"
            else -> "limit"
        }
    }

    // MapService's map is authoritative; events already in flight re-apply idempotently.
    private fun adoptBlocks(snapshot: MsAutoSnapshotDto) {
        _mapData.blocks = snapshot.blocks.map {
            BlockMap().apply {
                i = it.i
                j = it.j
                type = it.type
                hp = it.hp
                maxHp = it.maxHp
            }
        }.toMutableList()
        _mediator.saveLater(SAVE.MAP)
    }

    private fun treasureSnapshotPayload(snapshot: MsAutoSnapshotDto): ISFSObject {
        val modeName = _fixedMode.name.lowercase()
        val data = SFSObject()
        data.putUtfString("${SFSField.Datas}_$modeName", _mapData.toJson())
        data.putInt("${SFSField.Tileset}_$modeName", _mapData.tileset)
        data.putLong("seq", snapshot.seq)
        data.putLong("server_time", snapshot.serverTimeMs)
        data.putLong("fuse_ms", snapshot.fuseMs)
        data.putBool("awaiting_new_map", snapshot.awaitingNewMap)
        data.putLong("resume_at", snapshot.resumeAtMs)
        val heroes = SFSArray()
        for (h in snapshot.heroes) {
            heroes.addSFSObject(heroPosition(h.heroId, h.i, h.j))
        }
        data.putSFSArray("heroes", heroes)
        val bombs = SFSArray()
        for (b in snapshot.bombs) {
            val bomb = SFSObject()
            bomb.putLong(SFSField.ID, b.heroId.toLong())
            bomb.putInt("num", b.bombNo)
            bomb.putInt("i", b.i)
            bomb.putInt("j", b.j)
            bomb.putLong("planted_at", b.plantedAtMs)
            bomb.putLong("explode_at", b.explodeAtMs)
            bombs.addSFSObject(bomb)
        }
        data.putSFSArray("bombs", bombs)
        return data
    }

    private fun heroPosition(heroId: Int, i: Int, j: Int): ISFSObject {
        val obj = SFSObject()
        obj.putLong(SFSField.ID, heroId.toLong())
        _heroFiManager.getHero(heroId, _mediator.dataType)?.let { obj.putInt(SFSField.HeroType, it.type.value) }
        obj.putInt("i", i)
        obj.putInt("j", j)
        return obj
    }

    override fun onTreasureEvents(batch: MsTreasureEventBatch) {
        var rosterMayChange = false
        synchronized(locker) {
            var needNewMap = false
            for (e in batch.events) {
                val obj = when (e.type) {
                    MsTreasureEventType.MOVE -> treasureMove(e)
                    MsTreasureEventType.PLANT -> treasurePlant(e)
                    MsTreasureEventType.EXPLODE -> {
                        rosterMayChange = true
                        if (e.mapNowEmpty == true) needNewMap = true
                        treasureExplode(e)
                    }
                    MsTreasureEventType.HERO_JOIN, MsTreasureEventType.HERO_LEAVE -> heroPosition(e.heroId!!, e.i!!, e.j!!).also { obj ->
                        e.reason?.let { obj.putUtfString("reason", it) }
                    }
                    MsTreasureEventType.NEW_MAP -> treasureNewMap(e)
                    else -> null
                } ?: continue
                obj.putLong("seq", e.seq)
                obj.putUtfString("type", e.type)
                obj.putLong("at", e.atMs)
                _treasureOutbox.addSFSObject(obj)
            }
            scheduleTreasureFlush()
            if (needNewMap && _treasureActive) createNewMap(pushLegacyNewMap = false)
            if (rosterMayChange) rosterMayChange = _treasureRoster.keys != desiredTreasureHeroIds()
        }
        if (rosterMayChange) syncTreasureHeroes()
    }

    private fun desiredTreasureHeroIds(): Set<Int> = _heroFiManager.activeHeroes
        .filter { it.details.dataType == _mediator.dataType && it.stage == GameConstants.BOMBER_STAGE.WORK && it.energy > 0 }
        .sortedBy { it.heroId }
        .take(MAX_TREASURE_HEROES)
        .mapTo(HashSet()) { it.heroId }

    private fun treasureMove(e: MsTreasureEventDto): ISFSObject {
        val obj = heroPosition(e.heroId!!, e.i!!, e.j!!)
        val path = SFSArray()
        for (cell in e.path.orEmpty()) {
            val step = SFSObject()
            step.putInt("i", cell.i)
            step.putInt("j", cell.j)
            path.addSFSObject(step)
        }
        obj.putSFSArray("path", path)
        obj.putLong("step_ms", e.stepMs ?: 0)
        // Client roams the hero itself: 0 = open-ended, else back on (i, j) by this time.
        e.roamUntilMs?.let { obj.putLong("roam_until", it) }
        return obj
    }

    private fun treasurePlant(e: MsTreasureEventDto): ISFSObject {
        val obj = heroPosition(e.heroId!!, e.i!!, e.j!!)
        obj.putInt("num", e.bombNo!!)
        obj.putLong("planted_at", e.plantedAtMs ?: e.atMs)
        obj.putLong("explode_at", e.explodeAtMs ?: e.atMs)
        return obj
    }

    // The blast is always shown, credited or not, so the client map never drifts.
    private fun treasureExplode(e: MsTreasureEventDto): ISFSObject {
        val blocksHit = e.blocksHit.orEmpty()
        mirrorBlocksHit(blocksHit)
        if (blocksHit.isNotEmpty()) {
            _mediator.saveLater(SAVE.MAP)
        }
        val credited = if (e.takeResult == null || e.takeResult == "OK") {
            applyExplodeToHero(e, blocksHit)
        } else {
            null
        }
        val payload = credited ?: heroPosition(e.heroId!!, e.i!!, e.j!!).also {
            it.putInt("num", e.bombNo!!)
        }
        if (credited == null || (payload.getSFSArray(SFSField.Blocks).size() == 0 && blocksHit.isNotEmpty())) {
            // Hero no longer credited (asleep, no energy, gone): hp only, no rewards.
            val blocks = SFSArray()
            for (hit in blocksHit) {
                val block = SFSObject()
                block.putInt("i", hit.i)
                block.putInt("j", hit.j)
                block.putInt(SFSField.HP, hit.hp)
                blocks.addSFSObject(block)
            }
            payload.putSFSArray(SFSField.Blocks, blocks)
        }
        payload.putLong("planted_at", e.plantedAtMs ?: 0)
        payload.putBool("map_now_empty", e.mapNowEmpty ?: false)
        return payload
    }

    private fun treasureNewMap(e: MsTreasureEventDto): ISFSObject {
        val modeName = _fixedMode.name.lowercase()
        val obj = SFSObject()
        obj.putUtfString("${SFSField.Datas}_$modeName", _mapData.toJson())
        obj.putInt("${SFSField.Tileset}_$modeName", _mapData.tileset)
        val heroes = SFSArray()
        for (h in e.heroes.orEmpty()) {
            heroes.addSFSObject(heroPosition(h.heroId, h.i, h.j))
        }
        obj.putSFSArray("heroes", heroes)
        obj.putLong("resume_at", e.resumeAtMs ?: e.atMs)
        return obj
    }

    companion object {
        // Same cap as the client's map (DefaultPlayerManager / LevelView: 15 heroes).
        const val MAX_TREASURE_HEROES = 15
        const val TREASURE_MAP_RESET_PAUSE_MS = 3000L
        private const val TREASURE_KEEPALIVE_MS = 30_000
    }
}
