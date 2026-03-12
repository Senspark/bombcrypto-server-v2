package com.senspark.game.db.dailyMission

import com.senspark.common.service.IGlobalService
import com.senspark.game.data.model.user.AddUserItemWrapper
import com.senspark.game.data.model.user.UserMission
import com.senspark.game.declare.customEnum.MissionType

interface IMissionDataAccess : IGlobalService {

    /**
     * @return Map<missionCode, mission>
     */
    fun loadMission(uid: Int): MutableMap<String, UserMission>
    fun saveCompleteMission(
        uid: Int,
        missionType: MissionType,
        missionCode: String,
        numberMission: Int,
        completedMission: Int
    )

    fun saveReceivedReward(uid: Int, missionCode: String, rewardsReceived: List<AddUserItemWrapper>)
    fun checkUserAchievement(uid: Int, currentPvpRewardSeason: Int)
}

