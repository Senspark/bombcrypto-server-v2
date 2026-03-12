package com.senspark.game.data.model.config

import com.senspark.game.declare.customEnum.MissionAction
import com.senspark.game.declare.customEnum.MissionType
import com.senspark.game.utils.deserializeList
import java.sql.ResultSet

class Mission(
    override val code: String,
    override val type: MissionType,
    override val action: MissionAction,
    override val description: String,
    override val numberMission: Int,
    rewards: String,
    override val sort: Int,
    /**
     * cool down to next time (milliseconds)
     */
    override val coolDown: Int,
    override val nextMissionCode: String?,
    override val previousMissionCode: String?
) : IMission {
    override val reward: List<MissionReward> = deserializeList(rewards)
    
    companion object {
        fun fromResultSet(rs: ResultSet): Mission {
            return Mission(
                rs.getString("code"),
                MissionType.valueOf(rs.getString("type")),
                MissionAction.valueOf(rs.getString("action")),
                rs.getString("description"),
                rs.getInt("number_mission"),
                rs.getString("rewards"),
                rs.getInt("sort"),
                rs.getInt("cool_down"),
                rs.getString("next_mission_code"),
                rs.getString("previous_mission_code"),
            )
        }
    }
}