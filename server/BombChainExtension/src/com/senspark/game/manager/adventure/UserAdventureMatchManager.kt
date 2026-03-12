package com.senspark.game.manager.adventure

import com.senspark.common.constant.PvPItemType
import com.senspark.common.utils.toSFSArray
import com.senspark.game.constant.Booster
import com.senspark.game.data.AdventureMap
import com.senspark.game.data.manager.adventure.IAdventureEnemyConfigManager
import com.senspark.game.data.manager.adventure.IAdventureItemManager
import com.senspark.game.data.model.adventrue.AdventureBlockItem
import com.senspark.game.data.model.adventrue.UserAdventureMode
import com.senspark.game.data.model.config.AdventureBlock
import com.senspark.game.data.model.user.AdventureEnemy
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE.GOLD
import com.senspark.game.declare.ErrorCode
import com.senspark.game.exception.CustomException
import com.senspark.game.user.IAdventureHero
import com.senspark.game.utils.Utils
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.*
import kotlin.math.max
import kotlin.math.min

open class UserAdventureMatchManager(
    private val version: Int,
    private val adventureEnemyConfigManager: IAdventureEnemyConfigManager,
    private val adventureItemManager: IAdventureItemManager,
    private val userAdventureMode: UserAdventureMode,
    override val map: AdventureMap,
    override val stage: Int,
    override val level: Int,
    override val hero: IAdventureHero,
    private val _oneHit: Boolean,
    override var boosters: MutableMap<Booster, Int>,
) : IUserAdventureMatchManager {
    private val itemsReceived: MutableMap<PvPItemType, Int> = EnumMap(PvPItemType::class.java)
    private var explodeDoorCounter = 0
    private var lastExplodeDoorTimestamp = 0L
    private val startMapTimeStamp = Instant.now().toEpochMilli()
    private val enemyMap = map.enemies.associateBy { it.enemyId }.toMutableMap()
    private var finished = false
    private val doorX = map.door.x
    private val doorY = map.door.y
    override var reviveCount = 0

    /**
     * gold thuong roi ra tu quai trong map
     */
    private var goldReceive = 0


    override val strategy = map.strategy
    override val matchTimeInMiliSecond: Long get() = (Instant.now().toEpochMilli() - startMapTimeStamp)
    override var spawnEnemyOfDoor = false
    override val isFreeReviveHero = map.strategy.isFreeReviveHero

    val heroId get() = hero.heroId

    init {
//        remove item in door
        getBlockItem(doorX, doorY)?.let {
            map.items[doorX]!!.remove(doorY)
        }
    }

    override fun clearEnterDoor() {
        finished = false
    }

    override fun isBossLevel(): Boolean {
        return strategy.level == 5
    }

    override fun useBooster(booster: Booster, quantity: Int) {
        if (boosters.containsKey(booster)) {
            boosters[booster] = (boosters[booster] ?: 0) + quantity
        }
    }

    override fun enemyTakeDamage(enemyId: Int): Pair<Int, Int> {
        val enemy = getEnemy(enemyId) ?: throw CustomException("Cannot find enemy $enemyId")
        val damage = if (_oneHit) enemy.health.toInt() else hero.dmg
        // case enemy already dead
        if (enemy.health <= 0) {
            return Pair(0, 0)
        }
        enemy.subHealth(damage)
        var enemyGold = 0
        if (enemy.health <= 0) {
            enemyGold = if (isSecondTimePlayLevel()) {
                adventureEnemyConfigManager.getBySkin(enemy.skin).goldRewardOtherTime
            } else {
                adventureEnemyConfigManager.getBySkin(enemy.skin).goldRewardFirstTime
            }
            goldReceive += enemyGold
        }
        return Pair(damage, enemyGold)
    }

    private fun isSecondTimePlayLevel(): Boolean {
        return UserAdventureModeManager.isHigherLevel(
            stage,
            level,
            userAdventureMode.maxStage,
            userAdventureMode.maxLevel,
        ) || UserAdventureModeManager.isSameLevel(
            stage,
            level,
            userAdventureMode.maxStage,
            userAdventureMode.maxLevel,
        )
    }

    override fun enterDoor(): MutableMap<BLOCK_REWARD_TYPE, Int> {
        val rewardReceived = HashMap<BLOCK_REWARD_TYPE, Int>()
        finished = true
        itemsReceived.forEach {
            adventureItemManager.get(it.key)?.rewardType?.let { rewardType ->
                rewardReceived.compute(rewardType) { _, v ->
                    v?.plus(it.value) ?: it.value
                }
            }
        }
        if (goldReceive > 0) {
            //cộng gold rớt từ quái
            rewardReceived.compute(GOLD) { _, v ->
                v?.plus(goldReceive) ?: goldReceive
            }
        }
        
        return rewardReceived
    }

    override fun getAliveEnemyCount(): Int {
        return enemyMap.values.count { it.isAlive() }
    }

    override fun getKilledEnemyCount(): Int {
        return enemyMap.values.count { !it.isAlive() }
    }

    override fun getBlocksJson(): String {
        return map.blocks.map { it.value.map { it2 -> it2.value } }.flatten().toSFSArray { it.toSfsObject() }.toJson()
    }

    override fun getItemJson(): String {
        val itemList = map.items.map { it.value.map { it2 -> it2.value } }.flatten()
        return Json.encodeToString(itemList)
    }

    private fun getBlock(i: Int, j: Int): AdventureBlock? {
        return map.blocks[i]?.get(j)
    }

    private fun getBlockItem(i: Int, j: Int): AdventureBlockItem? {
        return map.items[i]?.get(j)
    }

    override fun getEnemy(enemyId: Int): AdventureEnemy? {
        return enemyMap[enemyId]
    }

    private fun getNextEnemyId(): Int {
        return enemyMap.size
    }

    override fun getNextEnemyCount(): Int {
        return enemyMap.size
    }

    override fun heroTakeDamage(value: Int) {
        hero.subHealth(value)
    }

    private fun isDoor(x: Int, y: Int): Boolean {
        return doorX == x && doorY == y
    }

    override fun isDoorOpened(): Boolean {
        return getBlock(doorX, doorY) == null
    }

    override fun isFinish(): Boolean {
        return finished
    }

    override fun killEnemy(enemyId: Int): Boolean {
        val enemy = getEnemy(enemyId) ?: return false
        enemy.die()
        return true
    }

    private fun removeBlock(blockMap: AdventureBlock?): Boolean {
        if (blockMap != null && map.blocks[blockMap.x]?.get(blockMap.y) != null) {
            map.blocks[blockMap.x]!!.remove(blockMap.y)
            return true
        }
        return false
    }

    private fun removeItem(i: Int, j: Int) {
        map.items[i]!!.remove(j)
    }

    override fun toAliveEnemyArray(): SFSArray {
        val result = SFSArray()
        enemyMap.values.filter { it.isAlive() }.forEach {
            result.addSFSObject(it.toObject())
        }
        return result
    }

    override fun toEnemyObject(): ISFSArray {
        val result = SFSArray()
        enemyMap.values.forEach {
            result.addSFSObject(it.toObject())
        }
        return result
    }

    override fun explode(
        timestamp: Long,
        clientBlocks: ISFSArray
    ): ISFSObject {
        val blocks = mutableListOf<AdventureBlock>()
        val items = mutableListOf<AdventureBlockItem>()
        val enemies = mutableListOf<AdventureEnemy>()
        val explodeBlock = fun(i: Int, j: Int) {
            val block = getBlock(i, j)
            val item = getBlockItem(i, j)
            if (block == null) {
                if (isDoor(i, j) and (timestamp > lastExplodeDoorTimestamp + 3 * 1000)) {
                    if (getKilledEnemyCount() == getEnemyCount()) {
                        spawnEnemyOfDoor = true
                    }
                    lastExplodeDoorTimestamp = timestamp
                    explodeDoorCounter++
                    enemies.addAll(
                        spawnEnemies(strategy.enemiesDoor, strategy.getEnemiesDoorNum(explodeDoorCounter))
                    )
                }
//                nếu block đã nổ mà có item thì xoá item
                if (item != null) {
                    items.add(item)
                }
                return
            }
            blocks.add(block)
        }
        for (i in 0 until clientBlocks.size()) {
            val obj = clientBlocks.getSFSObject(i)
            explodeBlock(obj.getInt("i"), obj.getInt("j"))
        }
        blocks.forEach { removeBlock(it) }
        items.forEach { removeItem(it.i, it.j) }
        val blocksResult = blocks.toSFSArray {
            SFSObject().apply {
                putInt("i", it.x)
                putInt("j", it.y)
            }
        }
        val itemsRemoved = items.toSFSArray {
            SFSObject().apply {
                putInt("i", it.i)
                putInt("j", it.j)
            }
        }
        return SFSObject().apply {
            putSFSArray("blocks", blocksResult)
            putSFSArray("enemies", enemies.toSFSArray { it.toObject() })
            putSFSArray("itemsRemoved", itemsRemoved)
        }
    }

    /**
     * sinh linh theo role mới (random quái trong level của stage)
     */
    private fun spawnEnemies(skins: List<Int>, numberEnemy: Int): List<AdventureEnemy> {
        val enemies = mutableListOf<AdventureEnemy>()
        for (i in 0 until numberEnemy) {
            val skin = skins[Utils.randInt(0, skins.size - 1)]
            enemies.addAll(spawnEnemies(skin, 1))
        }
        return enemies
    }

    override fun spawnEnemies(skin: Int, quantity: Int): List<AdventureEnemy> {
        val enemies = mutableListOf<AdventureEnemy>()
        for (j in 0 until quantity) {
            enemies.add(spawnEnemy(skin))
        }
        return enemies
    }

    private fun spawnEnemy(skin: Int): AdventureEnemy {
        val enemy = adventureEnemyConfigManager.generateStoryEnemy(getNextEnemyId(), skin)
        enemyMap[enemy.enemyId] = enemy
        return enemy
    }

    override fun getEnemyCount(): Int {
        return enemyMap.size
    }

    override fun takeItem(i: Int, j: Int): AdventureBlockItem {
        val block = getBlock(i, j)
        val item = getBlockItem(i, j)
        if (block != null) {
            throw CustomException("Block was not exploded", ErrorCode.INVALID_PARAMETER)
        }
        if (item == null) {
            throw CustomException("Item not exists", ErrorCode.INVALID_PARAMETER)
        }
        removeItem(i, j)
        hero.takeItem(item.itemType)
        itemsReceived.compute(item.itemType) { _, v ->
            val itemType = adventureItemManager.get(item.itemType)
            if (itemType?.rewardType == GOLD) {
                val currentValue = v ?: 0
                val remainValue = max(map.strategy.maxGoldReward - currentValue, 0)
                val addValue = min(item.rewardValue, remainValue)
                item.rewardValue = addValue
                currentValue + addValue
            } else {
                v?.plus(item.rewardValue) ?: item.rewardValue
            }
        }
        return item
    }

    override fun reviveHero() {
        reviveCount++
        hero.revive()
    }

}