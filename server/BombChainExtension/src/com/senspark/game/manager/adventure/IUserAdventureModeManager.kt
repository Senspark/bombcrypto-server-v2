package com.senspark.game.manager.adventure

import com.senspark.game.constant.Booster
import com.senspark.game.data.model.adventrue.UserAdventureMode
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.EnumConstants.DeviceType
import com.senspark.game.declare.EnumConstants.MatchResult
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import java.util.*

interface IUserAdventureModeManager {
    val matchManager: IUserAdventureMatchManager
    val userAdventureMode: UserAdventureMode
    fun enterDoor(): Triple<String, SFSArray, Boolean>
    fun getMap(version: Int, heroId: Int, stage: Int, level: Int, boosters: Set<Booster>): SFSObject
    fun takeItem(i: Int, j: Int): ISFSObject
    fun useBooster(booster: Booster)
    fun endGameAndSaveData(
        rewardsReceive: MutableMap<BLOCK_REWARD_TYPE, Int> = EnumMap(BLOCK_REWARD_TYPE::class.java),
        matchResult: MatchResult
    ): SFSArray

    suspend fun takeLuckyWheelReward(rewardId: String, adsToken: String, deviceType: DeviceType): ISFSObject

    /**
     * hàm này xử thua, trừ booster
     */
    fun clearOldMap()
    fun getReviveHeroCost(): ISFSObject?
    suspend fun reviveHero(adsToken: String?): MutableMap<String, Float>
}