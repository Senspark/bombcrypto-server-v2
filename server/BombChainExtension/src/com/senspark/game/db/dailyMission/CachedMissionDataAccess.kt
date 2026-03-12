package com.senspark.game.db.dailyMission

import com.senspark.common.cache.ICacheService
import com.senspark.game.constant.CachedKeys
import com.senspark.game.data.model.user.AddUserItemWrapper
import com.senspark.game.data.model.user.UserMission
import com.senspark.game.declare.customEnum.MissionType
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class CachedMissionDataAccess(
    private val _bridge: IMissionDataAccess,
    private val _cache: ICacheService
) : IMissionDataAccess {

    override fun initialize() {
    }
    
    /**
     * Read
     */
    override fun loadMission(uid: Int): MutableMap<String, UserMission> {
        val field = uid.toString()
        try {
            return Json.decodeFromString(MapSerializer(String.serializer(), UserMission.serializer()), _cache.getFromHash(CachedKeys.USER_MISSION, field)!!)
                .toMutableMap()
        } catch (e: Exception) {
            val result = _bridge.loadMission(uid)
            _cache.setToHash(CachedKeys.USER_MISSION, field, Json.encodeToString(MapSerializer(String.serializer(), UserMission.serializer()), result))
            return result
        }
    }

    /**
     * Write (Invalidate cache)
     */
    override fun saveCompleteMission(
        uid: Int,
        missionType: MissionType,
        missionCode: String,
        numberMission: Int,
        completedMission: Int
    ) {
        _bridge.saveCompleteMission(uid, missionType, missionCode, numberMission, completedMission)
        _cache.deleteFromHash(CachedKeys.USER_MISSION, uid.toString())
    }

    /**
     * Write (Invalidate cache)
     */
    override fun saveReceivedReward(uid: Int, missionCode: String, rewardsReceived: List<AddUserItemWrapper>) {
        _bridge.saveReceivedReward(uid, missionCode, rewardsReceived)
        _cache.deleteFromHash(CachedKeys.USER_MISSION, uid.toString())
    }

    /**
     * Write (Invalidate cache)
     */
    override fun checkUserAchievement(uid: Int, currentPvpRewardSeason: Int) {
        _bridge.checkUserAchievement(uid, currentPvpRewardSeason)
        _cache.deleteFromHash(CachedKeys.USER_MISSION, uid.toString())
    }

    private fun getCacheKey(uid: Int): String {
        return "${CachedKeys.USER_MISSION}$uid"
    }

}