package com.senspark.game.data.model.user

import com.senspark.common.utils.toSFSArray
import com.senspark.game.data.model.config.MissionReward
import com.senspark.game.declare.customEnum.MissionAction
import com.senspark.game.declare.customEnum.MissionType
import com.senspark.game.utils.deserializeList
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.Serializable
import java.sql.ResultSet
import java.time.Instant
import java.util.concurrent.TimeUnit

@Serializable
class UserMission(
    val uid: Int,
    val type: MissionType,
    var description: String? = null,
    val missionCode: String,
    var action: MissionAction? = null,
    var sort: Int? = null,
    val numberMission: Int,
    var completedMission: Int = 0,
    var isReceivedReward: Boolean = false,
    var rewards: List<MissionReward> = emptyList(),
    val rewardsReceived: List<UserMissionRewardReceived> = emptyList(),
    val modifyDate: Long = 0
) {
    val isCompleted: Boolean get() = completedMission >= numberMission
    private var coolDownEndAt: Long = 0
    val coolDownEnded: Boolean get() = Instant.now().toEpochMilli() > coolDownEndAt

    val remainCoolDown: String
        get() {
            val milliseconds = coolDownEndAt - Instant.now().toEpochMilli()
            val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) - hours * 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) - hours * 3600 - minutes * 60
            val results = mutableListOf<String>()
            if (hours > 0)
                results.add("${hours}h")
            if (minutes > 0) {
                results.add("${minutes}m")
            }
            if (seconds > 0) {
                results.add("${seconds}s")
            }
            return results.joinToString(" ")
        }

    companion object {
        fun fromResultSet(rs: ResultSet): UserMission {
            return UserMission(
                uid = rs.getInt("uid"),
                type = MissionType.valueOf(rs.getString("type")),
                missionCode = rs.getString("mission_code"),
                numberMission = rs.getInt("number_mission"),
                completedMission = rs.getInt("completed_mission"),
                isReceivedReward = rs.getInt("is_received_reward") == 1,
                rewardsReceived = if (rs.getString("rewards_received") == null) emptyList()
                else deserializeList(rs.getString("rewards_received")),
                modifyDate = rs.getLong("modify_date_milliseconds")
            )
        }
    }

    fun calculateCoolDown(previousMissionCoolDown: Int?, previousMissionModify: Long?): UserMission {
        coolDownEndAt = (previousMissionModify ?: 0) + (previousMissionCoolDown ?: 0)
        return this
    }

    fun toSfsObject(): SFSObject {
        return SFSObject.newInstance().apply {
            putUtfString("description", description)
            putUtfString("mission_code", missionCode)
            putInt("number_mission", numberMission)
            putInt("completed_mission", completedMission)
            putBool("is_received_reward", isReceivedReward)
            putSFSArray("rewards", rewards.toSFSArray { it.toSfsObject() })
            putSFSObject("reward", rewards.firstOrNull()?.toSfsObject() ?: SFSObject.newInstance())
            putSFSArray("rewards_received", rewardsReceived.toSFSArray { it.toSfsObject() })
            putLong("cool_down_end_at", coolDownEndAt)
        }
    }
}