package com.senspark.game.data.manager.dailyMission

import com.senspark.common.service.IServerService
import com.senspark.common.service.IService
import com.senspark.game.data.model.config.IMission
import com.senspark.game.declare.customEnum.MissionAction

interface IMissionManager : IService, IServerService {
    val missions: Map<String, IMission>
    val missionsList: List<IMission>
    fun getNextMission(action: MissionAction, currentMissionCode: String?): IMission
    fun getMission(missionCode: String): IMission
    fun getPreviousMission(currentMissionCode: String): IMission?
    fun getMissions(missionAction: MissionAction): List<IMission>

    /**
     * save user complete mission
     * @param actions list of pair<MissionActionType,quantity>
     */
    fun completeMission(uid: Int, actions: List<Pair<MissionAction, Int>>)
}