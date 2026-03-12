package com.senspark.game.manager.dailyMission

import com.senspark.game.data.model.user.AddUserItemWrapper
import com.senspark.game.data.model.user.UserMission
import com.senspark.game.declare.customEnum.MissionAction
import com.senspark.game.declare.customEnum.MissionType

class NullUserMissionManager : IUserMissionManager {
    override fun getTodayMissions(): Map<String, UserMission> {
        return emptyMap()
    }

    override fun getMissions(type: MissionType): List<UserMission> {
        return emptyList()
    }

    override fun completeMission(actions: List<Pair<MissionAction, Int>>) {}

    override suspend fun watchAds(missionCode: String, adsToken: String) {}

    override fun takeReward(missionCode: String): List<AddUserItemWrapper> {
        return emptyList()
    }
}