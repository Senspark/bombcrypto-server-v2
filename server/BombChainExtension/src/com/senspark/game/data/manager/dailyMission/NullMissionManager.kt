package com.senspark.game.data.manager.dailyMission

import com.senspark.game.data.model.config.IMission
import com.senspark.game.declare.customEnum.MissionAction
import com.senspark.game.exception.CustomException

class NullMissionManager : IMissionManager {

    override val missions: Map<String, IMission> get() = emptyMap()
    override val missionsList: List<IMission> get() = emptyList()

    override fun initialize() {
    }

    override fun getMission(missionCode: String): IMission {
        throw CustomException("Feature not support")
    }

    override fun getMissions(missionAction: MissionAction): List<IMission> {
        return emptyList()
    }

    override fun getNextMission(action: MissionAction, currentMissionCode: String?): IMission {
        throw CustomException("Feature not support")
    }

    override fun completeMission(uid: Int, actions: List<Pair<MissionAction, Int>>) {
        throw CustomException("Feature not support")
    }

    override fun getPreviousMission(currentMissionCode: String): IMission? {
        return null
    }

    override fun destroy() {}
}