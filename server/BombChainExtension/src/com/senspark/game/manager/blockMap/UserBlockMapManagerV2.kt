package com.senspark.game.manager.blockMap

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
import com.senspark.game.manager.blockMap.mapservice.IMapExplodeResultRouter
import com.senspark.game.manager.blockMap.mapservice.IMapServiceClient
import com.senspark.game.manager.blockMap.mapservice.MapServiceSessionNotFoundException
import com.senspark.game.manager.blockMap.mapservice.MsBlockDto
import com.senspark.game.manager.blockMap.mapservice.MsBlockHitDto
import com.senspark.game.manager.blockMap.mapservice.MsExplodeResultEvent
import com.senspark.game.manager.blockMap.mapservice.MsGameConfigDto
import com.senspark.game.manager.blockMap.mapservice.MsHeroSnapshotDto
import com.senspark.game.manager.blockMap.mapservice.MsMapInitRequest
import com.senspark.game.manager.blockMap.mapservice.MsMapReplaceRequest
import com.senspark.game.manager.blockMap.mapservice.MsPlantRequest
import com.senspark.game.manager.blockMap.mapservice.MsRewardConfigDto
import com.senspark.game.manager.blockMap.mapservice.MsRewardEntryDto
import com.senspark.game.manager.blockMap.mapservice.MsTargetHeroRequest
import com.senspark.game.manager.blockMap.mapservice.MsTargetsRequest
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

// Treasure-mode map manager that delegates target/plant/fuse/blast/reward logic to MapService.
// Explode results come back via AP_MAP_EXPLODE_RESULT_STR stream into [onExplodeResult].
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

    // Lazy: only resolved on first plant, so login never depends on it.
    private val _explodeResultRouter by lazy { _mediator.services.get<IMapExplodeResultRouter>() }

    @Volatile
    private var _registeredForExplodeResults = false

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
    private fun createNewMap() {
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
        }
        _mediator.sendDataEncryption(SFSCommand.PVE_NEW_MAP, SFSObject(), true)
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
            gameConfig = MsGameConfigDto(
                isCheckPlantMoveSpeed = _gameConfigManager.isCheckPlantMoveSpeed,
                isRejectPlantTooFast = _gameConfigManager.isRejectPlantTooFast,
                plantMoveSpeedToleranceMs = _gameConfigManager.plantMoveSpeedToleranceMs,
                plantMoveSpeedMultiplier = _gameConfigManager.plantMoveSpeedMultiplier,
                plantMoveSpeedMin = _gameConfigManager.plantMoveSpeedMin,
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
        if (_registeredForExplodeResults) {
            _explodeResultRouter.unregister(_sessionKey, this)
            _registeredForExplodeResults = false
        }
        if (!_sessionEverInitialized) return
        _coroutineScope.scope.launch(Dispatchers.IO) {
            try {
                _mapServiceClient.deleteSession(_sessionKey)
            } catch (e: Exception) {
                _mediator.logger.error("[MAP_SERVICE] failed to delete session $_sessionKey on logout", e)
            }
        }
    }

    // ================== V6 server-assigned bomb targeting ==================

    override fun getOrCreateTargets(requests: List<HeroTargetRequest>): List<HeroTargetResult> {
        if (requests.isEmpty()) return emptyList()
        return callMapService {
            val msRequest = MsTargetsRequest(requests.map {
                MsTargetHeroRequest(
                    heroId = it.heroId,
                    seedI = it.seed?.first,
                    seedJ = it.seed?.second,
                    rejectI = it.reject?.first,
                    rejectJ = it.reject?.second,
                )
            })
            val response = _mapServiceClient.getTargets(_sessionKey, msRequest)
            response.targets.map { HeroTargetResult(it.heroId, it.i to it.j) }
        }
    }

    @Throws(CustomException::class)
    override fun plantBomb(heroId: Int, bombNo: Int, col: Int, row: Int, speed: Int, bombCount: Int): PlantBombV2Outcome {
        val bbm = _heroFiManager.getHero(heroId, _mediator.dataType)
            ?: throw CustomException("Bomber man null", ErrorCode.BOMBERMAN_NULL)

        // Before the call, so the result stream can never beat the route to this manager.
        _explodeResultRouter.register(_sessionKey, this)
        _registeredForExplodeResults = true

        val outcome = callMapService {
            val response = _mapServiceClient.plantBomb(
                _sessionKey,
                MsPlantRequest(
                    heroId = heroId,
                    bombNo = bombNo,
                    i = col,
                    j = row,
                    speed = speed,
                    bombCount = bombCount,
                    hero = buildHeroSnapshot(bbm),
                    fuseMs = _gameConfigManager.timeBombExplode.toLong(),
                ),
            )
            if (response.result == PlantBombResult.OK.name && !response.fuseArmed) {
                // Outdated MapService without fuse support: bomb would never explode.
                _mediator.logger.error("[EXPLODE_V2] MapService accepted plant hero=$heroId bombNo=$bombNo pos=($col,$row) but armed no fuse")
            }
            PlantBombV2Outcome(
                result = PlantBombResult.valueOf(response.result),
                nextTarget = response.nextTarget?.let { it.i to it.j },
                hackFlag = response.isPlantTooFastHackFlag,
            )
        }
        if (outcome.hackFlag) {
            _mediator.tryToKickAndWriteLogHack(
                GameConstants.LOG_HACK_TYPE.HACK_SPEED,
                "bbmId:$heroId; plant bomb $bombNo at ($col,$row) flagged too-fast by MapService"
            )
        }
        return outcome
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

    // MapService already applied the blast, so the map is mirrored even if the hero checks fail.
    override fun onExplodeResult(event: MsExplodeResultEvent) {
        if (event.takeResult != "OK") {
            _mediator.logger.log("[EXPLODE_V2] explodeResult SKIP hero=${event.heroId} bombNo=${event.bombNo} reason=${event.takeResult}")
            return
        }
        synchronized(locker) {
            mirrorBlocksHit(event.blocksHit)
            if (event.blocksHit.isNotEmpty()) {
                _mediator.saveLater(SAVE.MAP)
            }
            val resultData = applyExplodeToHero(event)
            if (event.mapNowEmpty) {
                createNewMap()
            }
            if (resultData != null) {
                _mediator.sendDataEncryption(HandlerCommand.ResponseExplode, resultData, true)
            }
        }
    }

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

    // Returns RESPONSE_EXPLODE payload, or null to push nothing.
    private fun applyExplodeToHero(event: MsExplodeResultEvent): ISFSObject? {
        val heroId = event.heroId
        val bombNo = event.bombNo
        val col = event.i
        val row = event.j

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

        for (hit in event.blocksHit) {
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
                "fuseLatencyMs=${System.currentTimeMillis() - event.plantedAtMs}"
        )
        return resultData
    }
}
