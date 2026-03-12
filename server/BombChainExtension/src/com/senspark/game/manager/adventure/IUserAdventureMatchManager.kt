package com.senspark.game.manager.adventure

import com.senspark.game.constant.Booster
import com.senspark.game.data.AdventureMap
import com.senspark.game.data.model.adventrue.AdventureBlockItem
import com.senspark.game.data.model.config.LevelStrategy
import com.senspark.game.data.model.user.AdventureEnemy
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.user.IAdventureHero
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray

interface IUserAdventureMatchManager {
    val map: AdventureMap
    val hero: IAdventureHero
    val stage: Int
    val level: Int
    var spawnEnemyOfDoor: Boolean
    var boosters: MutableMap<Booster, Int>
    val matchTimeInMiliSecond: Long
    val reviveCount: Int
    val isFreeReviveHero: Boolean
    fun clearEnterDoor()
    fun isBossLevel(): Boolean

    /**
     * @return pair of dame and gold receive if enemy die
     */
    fun enemyTakeDamage(enemyId: Int): Pair<Int, Int>
    fun enterDoor(): MutableMap<BLOCK_REWARD_TYPE, Int>
    fun getAliveEnemyCount(): Int
    fun getKilledEnemyCount(): Int
    fun getBlocksJson(): String
    fun getItemJson(): String
    fun getEnemy(enemyId: Int): AdventureEnemy?
    fun getNextEnemyCount(): Int
    fun heroTakeDamage(value: Int)
    fun isDoorOpened(): Boolean
    fun isFinish(): Boolean
    fun killEnemy(enemyId: Int): Boolean
    fun toAliveEnemyArray(): SFSArray
    fun toEnemyObject(): ISFSArray
    fun explode(timestamp: Long, clientBlocks: ISFSArray): ISFSObject

    fun spawnEnemies(skin: Int, quantity: Int): List<AdventureEnemy>
    fun getEnemyCount(): Int
    fun takeItem(i: Int, j: Int): AdventureBlockItem
    fun reviveHero()
    fun useBooster(booster: Booster, quantity: Int = 1)
    val strategy: LevelStrategy
}