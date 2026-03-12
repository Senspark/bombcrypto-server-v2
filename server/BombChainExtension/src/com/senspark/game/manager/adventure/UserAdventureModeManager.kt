package com.senspark.game.manager.adventure

import com.senspark.game.api.IVerifyAdApiManager
import com.senspark.game.constant.Booster
import com.senspark.game.constant.Booster.*
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.AdventureMap
import com.senspark.game.data.manager.adventure.IAdventureEnemyConfigManager
import com.senspark.game.data.manager.adventure.IAdventureItemManager
import com.senspark.game.data.manager.adventure.IAdventureLevelConfigManager
import com.senspark.game.data.manager.adventure.IAdventureReviveHeroCostManager
import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.data.model.adventrue.AdventureBlockItem
import com.senspark.game.data.model.adventrue.UserAdventureMode
import com.senspark.game.data.model.config.AdventureBlock
import com.senspark.game.data.model.config.LevelStrategy
import com.senspark.game.data.model.config.Position
import com.senspark.game.data.model.user.AdventureEnemy
import com.senspark.game.data.model.user.RewardDetail
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.db.IGameDataAccess
import com.senspark.game.db.ILogDataAccess
import com.senspark.game.db.IRewardDataAccess
import com.senspark.game.declare.EnumConstants.*
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.customEnum.MissionAction
import com.senspark.game.exception.CustomException
import com.senspark.game.exception.HackException
import com.senspark.game.manager.ads.IUserBonusRewardManager
import com.senspark.game.manager.blockReward.IUserBlockRewardManager
import com.senspark.game.manager.dailyMission.IUserMissionManager
import com.senspark.game.manager.gachaChest.IUserGachaChestController
import com.senspark.game.manager.hero.IUserHeroTRManager
import com.senspark.game.manager.pvp.IUserBoosterManager
import com.senspark.game.manager.subscription.IUserSubscriptionManager
import com.senspark.game.user.AdventureHero
import com.senspark.game.user.IUserInventoryManager
import com.senspark.game.user.InitHeroStatCalculator
import com.senspark.lib.utils.Util
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

open class UserAdventureModeManager(
    private val _mediator: UserControllerMediator,
    private val heroManager: IUserHeroTRManager,
    private val userBlockRewardManager: IUserBlockRewardManager,
    private val userGachaChestManager: IUserGachaChestController,
    private val userInventoryManager: IUserInventoryManager,
    private val userMissionManager: IUserMissionManager,

    private val userBoosterManager: IUserBoosterManager,
    private val oneHit: Boolean,
    private val verifyAdApi: IVerifyAdApiManager,
    private val userSubscriptionManager: IUserSubscriptionManager,
    private val userBonusRewardManager: IUserBonusRewardManager,
    private val saveGameAndLoadReward: () -> Unit
) : IUserAdventureModeManager {

    private val configItemManager = _mediator.svServices.get<IConfigItemManager>()
    private val adventureEnemyConfigManager = _mediator.svServices.get<IAdventureEnemyConfigManager>()
    private val adventureLevelConfigManager = _mediator.svServices.get<IAdventureLevelConfigManager>()
    private val adventureItemManager = _mediator.svServices.get<IAdventureItemManager>()
    private val reviveHeroCostManager = _mediator.svServices.get<IAdventureReviveHeroCostManager>()
    
    private val dataAccessManager = _mediator.services.get<IDataAccessManager>()
    private val gameDataAccess: IGameDataAccess = dataAccessManager.gameDataAccess
    private val logDataAccess: ILogDataAccess = dataAccessManager.logDataAccess
    private val rewardDataAccess: IRewardDataAccess = dataAccessManager.rewardDataAccess
    
    override val userAdventureMode: UserAdventureMode by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        gameDataAccess.loadUserAdventureModeController(_mediator.userId) ?: UserAdventureMode()
    }
    private var _matchManager: IUserAdventureMatchManager? = null
    private val itemDensity = 55

    override val matchManager
        get() = _matchManager ?: throw HackException(11, false, "[UserAdventureModeManager].getMap")


    private fun generateAdventureMap(
        version: Int,
        stage: Int,
        level: Int,
        heroId: Int,
        boosters: Set<Booster>
    ) {
        checkSelectedMapValid(stage, level)
        val hero = heroManager.getHero(heroId)
        applyBooster(boosters)
        val storyMapHero = AdventureHero(
            hero,
            InitHeroStatCalculator(
                configItemManager,
                userInventoryManager.activeSkinChests,
                boosters,
                hero
            )
        )
        val map = when (version) {
            1 -> generateMap(stage, level)
            else -> generateMapV2(stage, level)
        }
        _matchManager = UserAdventureMatchManager(
            version,
            adventureEnemyConfigManager,
            adventureItemManager,
            userAdventureMode,
            map,
            stage,
            level,
            storyMapHero,
            oneHit,
            boosters.associateWith { 0 }.toMutableMap(),
        )
    }

    private fun applyBooster(boosters: Set<Booster>): Set<Booster> {
        val allowBoosters = setOf(RangePlusOne, BombPlusOne, SpeedPlusOne, Key, Shield)
//        mark as use booster
        boosters.forEach {
            if (allowBoosters.contains(it)) {
                userBoosterManager.chooseBooster(it.value, true)
            } else {
                throw CustomException("Not allow booster ${it.name}", ErrorCode.INVALID_PARAMETER)
            }
        }
        return boosters
    }

    private fun checkSelectedMapValid(stage: Int, level: Int) {
        val isSelectHigherMaxLevel =
            isHigherLevel(userAdventureMode.maxStage, userAdventureMode.maxLevel, stage, level)
        if (isSelectHigherMaxLevel) {
            val nextLevelOfMaxLevel = adventureLevelConfigManager.getNextLevel(
                userAdventureMode.maxStage,
                userAdventureMode.maxLevel
            )
            if (nextLevelOfMaxLevel == null || isHigherLevel(
                    nextLevelOfMaxLevel.stage,
                    nextLevelOfMaxLevel.level,
                    stage,
                    level
                )
            ) {
                throw CustomException("Selected level higher max level", ErrorCode.INVALID_PARAMETER)
            }
        }
    }

    private fun generateMap(stage: Int, level: Int): AdventureMap {
        val version = 1
        val strategy = adventureLevelConfigManager.getStrategy(stage, level)
        val blocks = createRandomMap(version, strategy)
        val items = createItemInBlock(blocks)
        val doorBlock = randomizeDoor(blocks)
        val enemies = mutableListOf<AdventureEnemy>()
        for (i in 0 until strategy.enemies.size) {
            val num: Int = strategy.enemiesNum[i]
            for (j in 0 until num) {
                enemies.add(adventureEnemyConfigManager.generateStoryEnemy(enemies.size, strategy.enemies[i]))
            }
        }
        return AdventureMap(
            blocks.mapValues { it.value.toMutableMap() }.toMutableMap(),
            doorBlock,
            Position(0, 0),
            enemies,
            strategy,
            items
        )
    }

    private fun generateMapV2(stage: Int, level: Int): AdventureMap {
        val version = 2
        val strategy = adventureLevelConfigManager.getStrategy(stage, level)
        val blocks: Map<Int, Map<Int, AdventureBlock>> = if (strategy.blocks == null) {
            createRandomMap(version, strategy)
        } else {
            strategy.blocks.groupBy { it.x }.mapValues { it.value.associateBy { it.y } }
        }
        val items = createItemInBlock(blocks)
        val door = strategy.door ?: randomizeDoor(blocks)

        val enemies = mutableListOf<AdventureEnemy>()
        if (strategy.enemiesV2 != null) {
            strategy.enemiesV2.forEach {
                enemies.add(
                    adventureEnemyConfigManager.generateStoryEnemy(
                        enemies.size,
                        it.enemyId,
                        it.randomizeSpawnPosition()
                    )
                )
            }
        } else {
            for (i in 0 until strategy.enemies.size) {
                val num: Int = strategy.enemiesNum[i]
                for (j in 0 until num) {
                    enemies.add(adventureEnemyConfigManager.generateStoryEnemy(enemies.size, strategy.enemies[i]))
                }
            }
        }
        val playerSpawn = strategy.playerSpawn ?: Position(0, 0)
        return AdventureMap(
            blocks.mapValues { it.value.toMutableMap() }.toMutableMap(),
            door,
            playerSpawn,
            enemies,
            strategy,
            items
        )
    }

    private fun randomizeDoor(blocks: Map<Int, Map<Int, AdventureBlock>>): Position {
        val blocksList = blocks.map { it.value.map { it2 -> it2.value } }.flatten()
        val doorIndex = Util.randInt(0, blocksList.size - 1)
        val doorBlock = blocksList[doorIndex]
        return Position(doorBlock.x, doorBlock.y)
    }

    private fun createRandomMap(
        version: Int,
        strategy: LevelStrategy
    ): Map<Int, Map<Int, AdventureBlock>> {
        val col = strategy.getCol(version)
        val row = strategy.getRow(version)
        val map = Array(col) { IntArray(row) }
        val blockMap: MutableMap<Int, MutableMap<Int, AdventureBlock>> = HashMap()
        //  Fill Wall
        for (i in 1 until col step 2) {
            for (j in 1 until row step 2) {
                map[i][j] = 1
            }
        }
        //  Random Brick
        for (i in 0 until col) {
            for (j in 0 until row) {
                // free cell for starting game
                if ((i == 0 && j == row - 1) || (i == 1 && j == row - 1) || (i == 0 && j == row - 2)) {
                    continue
                }
                if (map[i][j] == 0) {
                    val rand = Util.randFloat(0f, 1f)
                    if (rand < strategy.density) {
                        val bm = AdventureBlock(i, j)
                        val mapCols = blockMap[i] ?: HashMap()
                        mapCols[j] = bm
                        blockMap[i] = mapCols
                    }
                }
            }
        }
        return blockMap
    }

    private fun createItemInBlock(
        generatedBlockMap: Map<Int, Map<Int, AdventureBlock>>
    ): MutableMap<Int, MutableMap<Int, AdventureBlockItem>> {
        val bonusItemDensity = itemDensity + itemDensity * userSubscriptionManager.adventureBonusItems
        val itemMap: MutableMap<Int, MutableMap<Int, AdventureBlockItem>> = HashMap()
        var itemContainedChest = false
        generatedBlockMap.forEach {
            it.value.values.forEach { blockMap ->
                val rand = Util.randInt(1, 100)
                val i = blockMap.x
                val j = blockMap.y
                if (rand <= bonusItemDensity) {
                    val cols = itemMap[i] ?: mutableMapOf()
                    val itemType = adventureItemManager.getRandomItem(itemContainedChest)
                    if (itemType.type.isChest) {
                        itemContainedChest = true
                    }
                    cols[j] = AdventureBlockItem(
                        i,
                        j,
                        itemType.type,
                        itemType.type.value,
                        itemType.rewardValue
                    )
                    itemMap[i] = cols
                }
            }
        }
        return itemMap
    }

    override fun enterDoor(): Triple<String, SFSArray, Boolean> {
        val rewardId = UUID.randomUUID().toString()
        userBonusRewardManager.addRewardsAds(rewardId)
        val isBossLevel = matchManager.isBossLevel()
        val sfsArray = endGameAndSaveData(matchManager.enterDoor(), MatchResult.WIN)
        return Triple(rewardId, sfsArray, isBossLevel)
    }

    override fun endGameAndSaveData(
        rewardsReceive: MutableMap<BLOCK_REWARD_TYPE, Int>,
        matchResult: MatchResult
    ): SFSArray {
        saveGameAndLoadReward()
        //save reward nhận được
        val sfsArray = SFSArray()
        if (_matchManager != null) {
            if (matchResult == MatchResult.WIN) {
                //update user adventure mode
                updateUserAdventureMode()
                rewardsReceive.forEach {
                    val sfsObject = SFSObject()
                    sfsObject.putUtfString("rewardType", it.key.name)
                    sfsObject.putInt("value", it.value)
                    if (it.key.name.contains("chest", ignoreCase = true)) {
                        val result = userGachaChestManager.addChestFromBlockRewardType(it.key)
                        //nếu reward == null thì là hết slot, nên xoá khỏi ds reward
                        if (result == null) {
                            sfsObject.putBool("outOfSlot", true)
                        }
                    } else {
                        userBlockRewardManager.addReward(
                            RewardDetail(
                                it.key,
                                MODE.ADVENTURE,
                                DataType.TR,
                                it.value.toFloat()
                            )
                        )
                    }
                    sfsArray.addSFSObject(sfsObject)
                }
                saveGameAndLoadReward()
            }

            //tru booster
            userBoosterManager.saveUsedBooster()
            logDataAccess.logPlayPve(
                _mediator.userId,
                matchManager.stage,
                matchManager.level,
                matchResult,
                matchManager.matchTimeInMiliSecond,
                matchManager.boosters,
                matchManager.hero.heroId
            )
            // save complete mission
            userMissionManager.completeMission(listOf(Pair(MissionAction.WIN_ADVENTURE, 1)))
            //reset map
            _matchManager = null
        }
        return sfsArray
    }

    private fun updateUserAdventureMode() {
        val newStage = matchManager.stage
        val newLevel = matchManager.level
        userAdventureMode.currentStage = newStage
        userAdventureMode.currentLevel = newLevel
        userAdventureMode.heroId = matchManager.hero.heroId
        if (isHigherLevel(
                userAdventureMode.maxStage,
                userAdventureMode.maxLevel,
                newStage,
                newLevel
            )
        ) {
            this.userAdventureMode.maxStage = newStage
            this.userAdventureMode.maxLevel = newLevel
        }
        gameDataAccess.updateUserAdventureMode(_mediator.userId, userAdventureMode)
    }

    override fun clearOldMap() {
        if (_matchManager != null) {
            endGameAndSaveData(EnumMap(BLOCK_REWARD_TYPE::class.java), MatchResult.LOSE)
        }
    }

    override fun getMap(version: Int, heroId: Int, stage: Int, level: Int, boosters: Set<Booster>): SFSObject {
        if (_matchManager != null) {
            throw CustomException("Must complete map first")
        }
        generateAdventureMap(version, stage, level, heroId, boosters)
        return SFSObject().apply {
            putInt("stage", stage)
            putInt("level", level)
            putInt("row", matchManager.strategy.getRow(version))
            putInt("col", matchManager.strategy.getCol(version))
            putUtfString("positions", matchManager.getBlocksJson())
            putInt("door_x", matchManager.map.door.x)
            putInt("door_y", matchManager.map.door.y)
            putSFSObject("player_spawn", matchManager.map.playerSpawn.toSfsObject())
            putSFSArray("enemies", matchManager.toEnemyObject())
            putUtfString("items", matchManager.getItemJson())
            putSFSObject("hero", matchManager.hero.toSFSObject())
        }
    }

    override fun takeItem(i: Int, j: Int): ISFSObject {
        return SFSObject.newFromJsonData(Json.encodeToString(matchManager.takeItem(i, j)))
    }

    override fun useBooster(booster: Booster) {
        if (matchManager.boosters.contains(booster)) {
            // nếu hết máu thì không cho dùng shield
            if (booster == Shield && matchManager.hero.hp <= 0) {
                return
            }
            userBoosterManager.usePvpBooster(listOf(booster.value), false)
            matchManager.useBooster(booster)
        }
    }

    override suspend fun takeLuckyWheelReward(
        rewardId: String,
        adsToken: String,
        deviceType: DeviceType
    ): ISFSObject {
        return userBonusRewardManager.takeLuckyWheelReward(rewardId, adsToken, deviceType)
    }

    override fun getReviveHeroCost(): ISFSObject? {
        return if (!matchManager.isFreeReviveHero) {
            val reviveHeroCost = reviveHeroCostManager.getNextTimeCost(matchManager.reviveCount)
            if (reviveHeroCost != null) {
                SFSObject().apply {
                    putBool("allow_revive_by_ads", reviveHeroCost.allowAds)
                    putInt("revive_gem_amount", reviveHeroCost.gemAmount)
                }
            } else null
        } else {
            SFSObject().apply {
                putBool("allow_revive_by_ads", false)
                putInt("revive_gem_amount", 0)
            }
        }
    }

    override suspend fun reviveHero(adsToken: String?): MutableMap<String, Float> {
        val rewardsUsed = mutableMapOf<String, Float>()
        if (!matchManager.isFreeReviveHero) {
            if (matchManager.hero.hp > 0) {
                throw CustomException("Hero is still alive")
            }
            val isReviveByAds = !adsToken.isNullOrEmpty()
            val reviveCount = matchManager.reviveCount + 1
            val cost = reviveHeroCostManager.get(reviveCount)
            if (isReviveByAds && !cost.allowAds) {
                throw CustomException("Cannot use ads to revive hero")
            }
            if (!isReviveByAds && cost.gemAmount == 0) {
                throw CustomException("Cannot use gem to revive hero")
            }
            if (!isReviveByAds) {
                val gemHaving = userBlockRewardManager.getTotalGemHaving()
                val gemCost = cost.gemAmount
                if (gemHaving < gemCost) {
                    throw CustomException("Not enough $gemHaving ${BLOCK_REWARD_TYPE.GEM.name}")
                }
                val rewardUsed = rewardDataAccess.subUserGem(_mediator.userId, gemCost.toFloat())
                rewardsUsed.putAll(rewardUsed)
            } else {
                val isValid = verifyAdApi.isValidAds(adsToken)
                if (!isValid) {
                    throw CustomException("Ads invalid")
                }
            }
            saveGameAndLoadReward()
        }
        matchManager.reviveHero()
        return rewardsUsed
    }



    companion object {

        private fun padNumber(value: Int): String {
            return value.toString().padStart(2, '0')
        }

        /**
         * check level adventure
         *
         *  @return true if new > cur
         */
        fun isHigherLevel(curStage: Int, curLevel: Int, newStage: Int, newLevel: Int): Boolean {
            val curMapLevel = "${padNumber(curStage)}${padNumber(curLevel)}"
            val newMapLevel = "${padNumber(newStage)}${padNumber(newLevel)}"
            return newMapLevel > curMapLevel
        }

        /**
         * check level adventure
         *
         *  @return true if new > cur
         */
        fun isSameLevel(curStage: Int, curLevel: Int, newStage: Int, newLevel: Int): Boolean {
            return curStage == newStage && curLevel == newLevel
        }
    }
}