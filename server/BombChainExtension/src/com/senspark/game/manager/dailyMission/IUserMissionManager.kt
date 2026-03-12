package com.senspark.game.manager.dailyMission

import com.senspark.game.data.model.user.AddUserItemWrapper
import com.senspark.game.data.model.user.UserMission
import com.senspark.game.declare.customEnum.MissionAction
import com.senspark.game.declare.customEnum.MissionType

interface IUserMissionManager {
    fun getTodayMissions(): Map<String, UserMission>
    suspend fun watchAds(missionCode: String, adsToken: String)
    fun takeReward(missionCode: String): List<AddUserItemWrapper>
    fun completeMission(actions: List<Pair<MissionAction, Int>>)
    fun getMissions(type: MissionType): List<UserMission>
}