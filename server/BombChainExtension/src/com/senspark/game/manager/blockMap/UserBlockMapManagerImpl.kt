package com.senspark.game.manager.blockMap

import com.senspark.common.utils.LazyMutable
import com.senspark.game.controller.IUserController
import com.senspark.game.controller.MapData
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.manager.block.IBlockConfigManager
import com.senspark.game.data.manager.block.IBlockDropByDayManager
import com.senspark.game.data.manager.block.IBlockRewardDataManager
import com.senspark.game.data.model.config.IBlockReward
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.user.BlockMap
import com.senspark.game.data.model.user.RewardDetail
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.declare.*
import com.senspark.game.declare.EnumConstants.*
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE.BCOIN
import com.senspark.game.declare.GameConstants.ENERGY_DANGEROUS
import com.senspark.game.declare.GameConstants.MAP_MAX_COL
import com.senspark.game.declare.GameConstants.MAP_MAX_ROW
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.blockReward.IUserBlockRewardManager
import com.senspark.game.manager.stake.IHeroStakeManager
import com.senspark.game.manager.treasureHuntV2.ITreasureHuntV2Manager
import com.senspark.game.manager.treasureHuntV2.UserId
import com.senspark.game.utils.Utils
import com.senspark.lib.data.manager.IGameConfigManager
import com.senspark.lib.utils.Util
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class UserBlockMapManagerImpl(
    private val _mediator: UserControllerMediator,
    private val _blockRewardManager: IUserBlockRewardManager,
) : IUserBlockMapManager {

    private val _blockConfigManager = _mediator.services.get<IBlockConfigManager>()
    private val _dataAccessManager = _mediator.services.get<IDataAccessManager>()
    private val _blockDropRateManager = _mediator.services.get<IBlockDropByDayManager>()
    private val _blockRewardDataManager = _mediator.services.get<IBlockRewardDataManager>()
    private val _gameConfigManager = _mediator.services.get<IGameConfigManager>()
    
    private val _treasureHuntV2Manager = _mediator.svServices.get<ITreasureHuntV2Manager>()
    private val _heroStakeManager = _mediator.svServices.get<IHeroStakeManager>()

    private var _mapData: MapData by LazyMutable { initMapData() }
    private val _fixedMode = MODE.PVE_V2
    override val locker = Any()

    // ================== BLOCK MAP ==================

    private fun initMapData(): MapData {
        val m = _dataAccessManager.gameDataAccess.loadSingleMapData(
            _mediator.userId,
            _mediator.dataType,
            _fixedMode
        )
            ?: initFirstMap()
        return m
    }

    override fun canSetBoom(col: Int, row: Int): Boolean {
        val result = _mapData.canSetBoom(col, row)
        return result
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

    override fun explode(bbm: Hero, colBoom: Int, rowBoom: Int, blockArr: ISFSArray): ISFSObject {
        val blockExplode: List<BlockMap> = getBlockExplode(bbm, colBoom, rowBoom)
        val resultData: ISFSObject = if (_mediator.dataType.isAirdropUser()) {
            startExplodeTon(bbm, blockExplode, blockArr)
        } else {
            startExplodeLegacy(bbm, blockExplode, blockArr)
        }

        val mapIsEmpty = _mapData.isEmpty()

        if (mapIsEmpty) {
            createNewMap()
        }
        return resultData
    }

    private fun startExplodeLegacy(
        bbm: Hero,
        blocks: List<BlockMap>,
        blockArr: ISFSArray
    ): ISFSObject {
        //trừ năng lượng
        bbm.subEnergy()
        val damTreasure = bbm.damageTreasure + bbm.totalPower
        val damJail = bbm.damageJail + bbm.totalPower
        val resultData: ISFSObject = SFSObject()
        resultData.putLong(SFSField.ID, bbm.heroId.toLong())
        resultData.putInt(SFSField.Energy, bbm.energy)
        val blocksResult: ISFSArray = SFSArray()
        resultData.putSFSArray(SFSField.Blocks, blocksResult)

        //push trước ghost block vào, block bất đồng bộ client server
        val ghostBlock: ISFSArray = getGhostBlock(blockArr)
        for (i in 0 until ghostBlock.size()) {
            blocksResult.addSFSObject(ghostBlock.getSFSObject(i))
        }
        //bắt đầu nổ block
        val mapReward: MutableMap<BLOCK_REWARD_TYPE, RewardDetail> = EnumMap(BLOCK_REWARD_TYPE::class.java)
        var attendPools = listOf<Int>()
        var isDamTreasure = false
        for (bm in blocks) {
            //trừ máu block
            if (bm.type == GameConstants.BLOCK_TYPE.JAIL) {
                bm.subHP(damJail)
            } else {
                bm.subHP(damTreasure)
                isDamTreasure = true
            }

            //push vào data trả về client
            val blockResult: ISFSObject = SFSObject()
            blockResult.putInt("i", bm.i)
            blockResult.putInt("j", bm.j)
            blockResult.putInt(SFSField.HP, bm.hp)
            blocksResult.addSFSObject(blockResult)

            //kiểm tra xem block còn máu không tra thưởng và remove block đó ra
            if (bm.hp > 0) {
                continue
            }
            _mapData.removeBlockMap(bm)

            //get quà ra push vào phần thưởng trà về client
            val blockRewards = getRewards(bbm, _mediator.dataType, bm.type)

            if (blockRewards.isNotEmpty()) {
                val rewards: ISFSArray = SFSArray()
                blockResult.putSFSArray(SFSField.Rewards, rewards)
                for (blockReward in blockRewards) {
                    if (blockReward.getValue(_mediator.dataType) <= 0) {
                        continue
                    }
                    // so luong duoc cong
                    val rewardValueByMode = getValueBlockReward(blockReward)

                    val rw: ISFSObject = SFSObject()
                    rw.putUtfString(SFSField.Type, blockReward.type.name)
                    rw.putFloat(SFSField.Value, rewardValueByMode)
                    rewards.addSFSObject(rw)

                    //cộng phần thưởng
                    val type = blockReward.type
                    if (!mapReward.containsKey(type)) {
                        mapReward[type] = RewardDetail(type, _fixedMode, _mediator.dataType, 0f)
                    }
                    val rewardDetail = mapReward[type]
                    rewardDetail!!.addValue(rewardValueByMode)
                    mapReward[type] = rewardDetail
                }
            }
            if (bm.type != GameConstants.BLOCK_TYPE.JAIL && bm.type != GameConstants.BLOCK_TYPE.NORMAL) {
                attendPools = _treasureHuntV2Manager.addHeroToPool(bbm, UserId(_mediator.userId, _mediator.userName, _mediator.dataType))
            }
        }
        resultData.putIntArray("attend_pools", attendPools)

        _blockRewardManager.addRewards(mapReward)
        _mediator.saveLater(SAVE.REWARD)

        resultData.putInt(SFSField.IsDangerous, 0)
        //Tinh ty le nguy hiem
        if (bbm.isDangerous) {
            //check hero co skill tranh tham hoa khong, neu khong co  tru het nang luong
            val isAvoid = bbm.isAvoid(isDamTreasure)
            if (!isAvoid) {
                //tru het nang luong va di ngu
                bbm.killBomberman()
            }
            //put lai gia tri energy da tru
            resultData.putInt(SFSField.Energy, bbm.energy)
            resultData.putInt(
                SFSField.IsDangerous, when (isAvoid) {
                    true -> 2
                    false -> 1
                }
            )
        }
        //TODO tru stamina shield tranh thoat hiem, tam thoi chi tru stamina shiled tranh sam set
        bbm.subStaminaShield(isDamTreasure)

        resultData.putBool("is_trial", false)
        return resultData
    }

    private fun startExplodeTon(
        bbm: Hero,
        blocks: List<BlockMap>,
        blockArr: ISFSArray,
    ): ISFSObject {
        //trừ năng lượng
        bbm.subEnergy()
        val damTreasure = bbm.damageTreasure + bbm.totalPower
        val damJail = bbm.damageJail + bbm.totalPower
        val resultData: ISFSObject = SFSObject()
        resultData.putLong(SFSField.ID, bbm.heroId.toLong())
        resultData.putInt(SFSField.Energy, bbm.energy)
        val blocksResult: ISFSArray = SFSArray()
        resultData.putSFSArray(SFSField.Blocks, blocksResult)

        //push trước ghost block vào, block bất đồng bộ client server
        val ghostBlock: ISFSArray = getGhostBlock(blockArr)
        for (i in 0 until ghostBlock.size()) {
            blocksResult.addSFSObject(ghostBlock.getSFSObject(i))
        }
        //bắt đầu nổ block
        val mapReward: MutableMap<BLOCK_REWARD_TYPE, RewardDetail> = EnumMap(BLOCK_REWARD_TYPE::class.java)
        for (bm in blocks) {
            //trừ máu block
            if (bm.type == GameConstants.BLOCK_TYPE.JAIL) {
                bm.subHP(damJail)
            } else {
                bm.subHP(damTreasure)
            }

            //push vào data trả về client
            val blockResult: ISFSObject = SFSObject()
            blockResult.putInt("i", bm.i)
            blockResult.putInt("j", bm.j)
            blockResult.putInt(SFSField.HP, bm.hp)
            blocksResult.addSFSObject(blockResult)

            //kiểm tra xem block còn máu không tra thưởng và remove block đó ra
            if (bm.hp > 0) {
                continue
            }
            _mapData.removeBlockMap(bm)

            val rewardCanReceived = mutableListOf(BLOCK_REWARD_TYPE.COIN, BLOCK_REWARD_TYPE.BOMBERMAN)
            val blockRewards = _blockRewardDataManager.getRewards(_mediator.dataType, rewardCanReceived, bm.type)

            if (blockRewards.isNotEmpty()) {
                val rewards: ISFSArray = SFSArray()
                blockResult.putSFSArray(SFSField.Rewards, rewards)
                for (blockReward in blockRewards) {
                    val rewardValueByMode = blockReward.getValue(_mediator.dataType)
                    if (rewardValueByMode <= 0) {
                        continue
                    }
                    val rw: ISFSObject = SFSObject()
                    rw.putUtfString(SFSField.Type, blockReward.type.name)
                    rw.putFloat(SFSField.Value, rewardValueByMode)
                    rewards.addSFSObject(rw)

                    //cộng phần thưởng
                    val type = blockReward.type
                    if (!mapReward.containsKey(type)) {
                        mapReward[type] = RewardDetail(type, _fixedMode, _mediator.dataType, 0f)
                    }
                    val rewardDetail = mapReward[type]
                    rewardDetail!!.addValue(rewardValueByMode)
                    mapReward[type] = rewardDetail
                }
            }
        }
        _blockRewardManager.addRewards(mapReward)
        _mediator.saveLater(SAVE.REWARD)

        resultData.putIntArray("attend_pools", listOf<Int>())
        resultData.putInt(SFSField.HeroType, bbm.type.value)
        return resultData
    }

    private fun getGhostBlock(blockArr: ISFSArray): ISFSArray {
        //check them truong hop bat dong bo client vi ly do gi do client ko no block khi sv tra ve
        val blocksResult: ISFSArray = SFSArray()
        val size = blockArr.size()
        for (i in 0 until size) {
            val blockObject = blockArr.getSFSObject(i)
            val col = blockObject.getInt("i")
            val row = blockObject.getInt("j")
            val bm = _mapData.getBlockMap(col, row)
            if (bm == null) {
                val blockResult: ISFSObject = SFSObject()
                blockResult.putInt("i", col)
                blockResult.putInt("j", row)
                blockResult.putInt(SFSField.HP, 0)
                blocksResult.addSFSObject(blockResult)
            }
        }
        return blocksResult
    }

    @Throws(CustomException::class)
    override fun getBlockMap(): ISFSObject {
        val hasAnyBlock = _mapData.containBlockHPLeft()
        if (!hasAnyBlock) {
            createNewMap()
        }

        val data: ISFSObject = SFSObject()
        val modeName = _fixedMode.name.lowercase()
        data.putUtfString("${SFSField.Datas}_${modeName}", _mapData.toJson())
        data.putInt("${SFSField.Tileset}_${modeName}", _mapData.tileset)
        return data
    }

    private fun createNewMap() {
        val mapData = createRandomMap(_fixedMode)
        val hasAnyBlock = mapData.containBlockHPLeft()
        if (!hasAnyBlock) {
            throw CustomException("Server Error x043 ", ErrorCode.CREATE_MAP_FAIL)
        }
        _mapData = mapData
        _mediator.saveLater(SAVE.MAP)
        _mediator.sendDataEncryption(SFSCommand.PVE_NEW_MAP, SFSObject(), true)
    }

    private fun initFirstMap(): MapData {
        val mapData = createRandomMap(_fixedMode)
        val hasAnyBlock = mapData.containBlockHPLeft()
        if (!hasAnyBlock) {
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

    private fun createRandomMap(pveMode: MODE): MapData {
        val col = MAP_MAX_COL
        val row = MAP_MAX_ROW
        val map = Array(col) { IntArray(row) }
        val dayPassed: Int = getDayPassed()
        val blockDropRate = _blockDropRateManager.getBlockDropRate(_mediator.dataType, dayPassed)
        val mapdata = MapData()
        // Đặt ô tường cố định trong map 0,1
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

        // Đặt random ô gạch
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
        val titleset: Int
        if (_mediator.dataType != DataType.TON && _mediator.dataType != DataType.SOL) {
            titleset = Utils.randInt(0, _gameConfigManager.maxTitleset)
        } else {
            titleset = Utils.randInt(0, _gameConfigManager.maxTitleset * 2 + 1)
        }
        mapdata.tileset = titleset
        mapdata.mode = pveMode
        return mapdata
    }

    private fun createBlockMap(i: Int, j: Int, blockDroprate: List<Int>): BlockMap {
        //int[] blockRand = new int[] { 74, 7, 10, 4, 3, 2, 0 };	//TODO get from DB
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

    override fun getBlockExplode(bbm: Hero, colBoom: Int, rowBoom: Int): List<BlockMap> {
        val blocks: MutableList<BlockMap> = ArrayList()
        val bombRange = bbm.bombRange
        val pieceBlock = bbm.containsAbility(GameConstants.BOMBER_ABILITY.PIERCE_BLOCK)
        var block: BlockMap?
        //left check tu vi tri đặt bom trừ đi 1
        var minCol = colBoom - bombRange
        minCol = if (minCol < 0) 0 else minCol
        for (col in colBoom - 1 downTo minCol) {
            if (col % 2 == 1 && rowBoom % 2 == 1) {
                break // đụng tường ko check tiếp
            }
            block = _mapData.getBlockMap(col, rowBoom)
            if (block != null) {
                blocks.add(block)
            }
            if (block != null && !pieceBlock) {
                break
            }
        }
        //right check tu vi tri đặt bom cộng thêm 1
        var maxCol = colBoom + bombRange
        maxCol = if (maxCol >= MAP_MAX_COL) MAP_MAX_COL - 1 else maxCol
        for (col in colBoom + 1..maxCol) {
            if (col % 2 == 1 && rowBoom % 2 == 1) {
                break
            }
            block = _mapData.getBlockMap(col, rowBoom)
            if (block != null) {
                blocks.add(block)
            }
            if (block != null && !pieceBlock) {
                break
            }
        }
        //up
        var minRow = rowBoom - bombRange
        minRow = if (minRow < 0) 0 else minRow
        for (row in rowBoom - 1 downTo minRow) {
            if (colBoom % 2 == 1 && row % 2 == 1) {
                break
            }
            block = _mapData.getBlockMap(colBoom, row)
            if (block != null) {
                blocks.add(block)
            }
            if (block != null && !pieceBlock) {
                break
            }
        }
        //down
        var maxRow = rowBoom + bombRange
        maxRow = if (maxRow >= MAP_MAX_ROW) MAP_MAX_ROW - 1 else maxRow
        for (row in rowBoom + 1..maxRow) {
            if (colBoom % 2 == 1 && row % 2 == 1) {
                break
            }
            block = _mapData.getBlockMap(colBoom, row)
            if (block != null) {
                blocks.add(block)
            }
            if (block != null && !pieceBlock) {
                break
            }
        }
        return blocks
    }

    // ================== BLOCK MAP ==================

    /**
     * Ko liên quan đến BlockMap
     */
    override fun getBombermanDangerous(controller: IUserController): SFSObject {
        val results = SFSObject()
        val arraySFSObject = SFSArray()
        controller.masterUserManager.heroFiManager.activeHeroes.forEach {
            arraySFSObject.addSFSObject(
                getBombermanDangerousStatus(it)
            )
        }
        results.putSFSArray("dangerous", arraySFSObject)
        return results
    }

    /**
     * Ko liên quan đến BlockMap
     */
    override fun getBombermanDangerousStatus(hero: Hero): SFSObject {
        val results = SFSObject()
        //set default
        results.putInt(SFSField.Energy, hero.energy)
        results.putLong(SFSField.ID, hero.heroId.toLong())
        results.putInt(SFSField.IsDangerous, 0)
        results.putInt(SFSField.HeroType, hero.type.value)
        //check bomber active va lam viec co nang luong < 6
        if (hero.isActive && hero.stage == GameConstants.BOMBER_STAGE.WORK && hero.energy < ENERGY_DANGEROUS) {
            if (hero.isDangerous) {
                //check hero co skill tranh tham hoa khong, neu khong co  tru het nang luong
                val isAvoid = hero.isAvoid(true)
                if (!isAvoid) {
                    //tru het nang luong va di ngu
                    hero.killBomberman()
                }
                //put lai gia tri energy da tru
                results.putInt(SFSField.Energy, hero.energy)
                results.putInt(
                    SFSField.IsDangerous, when (isAvoid) {
                        true -> 2
                        false -> 1
                    }
                )
            }
        }
        return results
    }

    /**
     * Ko liên quan đến BlockMap
     */
    private fun getRewards(bbm: Hero, dataType: DataType, blockType: Int): List<IBlockReward> {
        val minStakeHeroConfig = _heroStakeManager.minStakeHeroConfig
        var stakeBcoin = bbm.stakeBcoin
        if (!bbm.isHeroS) {
            stakeBcoin -= minStakeHeroConfig[bbm.rarity]?.toFloat() ?: 0f
        }
        // Luôn có thể nhận được coin và bomberman
        val listRewardTypeRandom = arrayListOf(BLOCK_REWARD_TYPE.COIN, BLOCK_REWARD_TYPE.BOMBERMAN)
        // Nếu đủ stake bcoin thì nhận được bcoin
        if (stakeBcoin >= _gameConfigManager.minStakeBcoinTHV1[bbm.rarity]) {
            listRewardTypeRandom.add(BCOIN)
        }

        // Nếu đủ stake sen thì có thể nhận được sen
        val stakeSen = bbm.stakeSen
        if (stakeSen >= _gameConfigManager.minStakeSenTHV1[bbm.rarity]) {
            listRewardTypeRandom.add(BLOCK_REWARD_TYPE.SENSPARK)
        }
        return _blockRewardDataManager.getRewards(dataType, listRewardTypeRandom, blockType)
    }

    private fun getDayPassed(): Int {
        return 1
    }

    private fun getValueBlockReward(blockReward: IBlockReward): Float {
        return blockReward.getValue(_mediator.dataType)
    }

    /**
     * Ko liên quan đến BlockMap
     */
    override fun checkHackExplodeBlock(bbm: Hero, blockArr: ISFSArray): Boolean {
        val blockExplode = blockArr.size()
        var maxBlockCanExplode = 4
        if (bbm.containsAbility(GameConstants.BOMBER_ABILITY.PIERCE_BLOCK)) {
            maxBlockCanExplode = bbm.bombRange * 4
        }
        if (blockExplode > maxBlockCanExplode) {
            val data =
                "bbmId:" + bbm.heroId + "; maxBlockCanExplode:" + maxBlockCanExplode + "; blockExplode:" + blockExplode
            return _mediator.tryToKickAndWriteLogHack(GameConstants.LOG_HACK_TYPE.HACK_EXPLODE_BLOCK, data)
        }
        return false
    }

    /**
     * Ko liên quan đến BlockMap
     */
    override fun checkHackSpeedBombExplode(bbm: Hero, bombNo: Int): Boolean {
        if (bombNo > bbm.bombCount) {
            val data = "bbmId:" + bbm.heroId + "; Bomb no " + bombNo + " greater than total bom: " + bbm.bombCount
            _mediator.tryToKickAndWriteLogHack(GameConstants.LOG_HACK_TYPE.HACK_SPEED, data)
            return true
        }
        val hashExplode = bbm.hashBombExplode
        if (!hashExplode.containsValue(bombNo.toLong())) {
            hashExplode[bombNo] = System.currentTimeMillis()
        } else {
            val currTime = System.currentTimeMillis()
            val lastExplode = hashExplode[bombNo]!!
            val timeDiff = currTime - lastExplode
            val timeExplode = _gameConfigManager.timeBombExplode
            if (timeDiff < timeExplode) {
                val data = "The time between 2 bombings is too short: $timeDiff"
                return _mediator.tryToKickAndWriteLogHack(GameConstants.LOG_HACK_TYPE.HACK_SPEED, data)
            }
            hashExplode.replace(bombNo, currTime)
        }
        return false
    }
}