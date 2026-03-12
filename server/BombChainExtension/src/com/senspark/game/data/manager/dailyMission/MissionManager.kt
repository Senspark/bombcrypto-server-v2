package com.senspark.game.data.manager.dailyMission

import com.senspark.game.data.model.config.IMission
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.db.dailyMission.IMissionDataAccess
import com.senspark.game.declare.customEnum.MissionAction
import com.senspark.game.exception.CustomException

class MissionManager(
    private val _shopDataAccess: IShopDataAccess,
    private val _missionDataAccess: IMissionDataAccess
) : IMissionManager {

    override val missions: MutableMap<String, IMission> = mutableMapOf()
    override val missionsList: MutableList<IMission> = mutableListOf()

    override fun initialize() {
        missions.putAll(_shopDataAccess.loadDailyMission())
        missionsList.addAll(missions.values.toList())
    }

    override fun getMission(missionCode: String): IMission {
        return missions[missionCode] ?: throw CustomException("Mission with code $missionCode invalid")
    }

    override fun getMissions(missionAction: MissionAction): List<IMission> {
        return missionsList.filter { it.action == missionAction }
    }

    override fun getNextMission(action: MissionAction, currentMissionCode: String?): IMission {
        // case chưa làm nhiệm vụ nào
        if (currentMissionCode == null) {
            return missionsList.filter { it.action == action }.minByOrNull { it.sort }
                ?: throw CustomException("Cannot find next mission")
        }
        val currentMission = getMission(currentMissionCode)
        return missions[currentMission.nextMissionCode] ?: throw CustomException("Cannot find next mission")
    }

    override fun completeMission(uid: Int, actions: List<Pair<MissionAction, Int>>) {
        actions.forEach {
            getMissions(it.first).forEach { mission ->
                _missionDataAccess.saveCompleteMission(
                    uid,
                    mission.type,
                    mission.code,
                    mission.numberMission,
                    it.second
                )
            }
        }
    }

    override fun getPreviousMission(currentMissionCode: String): IMission? {
        val currentMission = getMission(currentMissionCode)
        return missions[currentMission.previousMissionCode]
    }

    override fun destroy() {
    }
}