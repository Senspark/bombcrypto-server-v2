package com.senspark.game.data.model.config

import com.senspark.game.declare.customEnum.MissionAction
import com.senspark.game.declare.customEnum.MissionType

interface IMission {
    val code: String
    val type: MissionType
    val action: MissionAction
    val description: String
    val numberMission: Int
    val sort: Int

    /**
     * cool down to next time (milliseconds)
     */
    val coolDown: Int
    val nextMissionCode: String?
    val previousMissionCode: String?
    val reward: List<MissionReward>
} 