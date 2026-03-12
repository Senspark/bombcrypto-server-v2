package com.senspark.game.manager.adventure

import com.senspark.game.constant.Booster
import com.senspark.game.data.model.adventrue.UserAdventureMode
import com.senspark.game.declare.EnumConstants.*
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

open class NullUserAdventureModeManager : IUserAdventureModeManager {

    override val userAdventureMode: UserAdventureMode by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        UserAdventureMode()
    }

    override val matchManager get() = throw CustomException("Feature not support")

    override fun enterDoor(): Triple<String, SFSArray, Boolean> {
        return Triple("", SFSArray(), false)
    }

    override fun endGameAndSaveData(
        rewardsReceive: MutableMap<BLOCK_REWARD_TYPE, Int>,
        matchResult: MatchResult
    ): SFSArray {
        return SFSArray()
    }

    override fun clearOldMap() {}

    override fun getMap(version: Int, heroId: Int, stage: Int, level: Int, boosters: Set<Booster>): SFSObject {
        return SFSObject()
    }

    override fun takeItem(i: Int, j: Int): ISFSObject {
        return SFSObject()
    }

    override fun useBooster(booster: Booster) {}

    override suspend fun takeLuckyWheelReward(
        rewardId: String,
        adsToken: String,
        deviceType: DeviceType
    ): ISFSObject {
        return SFSObject()
    }

    override fun getReviveHeroCost(): ISFSObject? {
        return SFSObject()
    }

    override suspend fun reviveHero(adsToken: String?): MutableMap<String, Float> {
        return mutableMapOf()
    }
}