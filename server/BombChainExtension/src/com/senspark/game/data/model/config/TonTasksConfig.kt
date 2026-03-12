package com.senspark.game.data.model.config

import com.senspark.game.manager.ton.TaskType
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import java.sql.ResultSet

class TonTasksConfig(
    val id: Int,
    val type: TaskType,
    val reward: Int
) {
    companion object {
        fun fromResultSet(rs: ResultSet): TonTasksConfig {
            return TonTasksConfig(
                rs.getInt("id"),
                TaskType.fromInt(rs.getInt("type"))!!,
                rs.getInt("reward")
            )
        }
    }

    fun toSFSObject(): ISFSObject {
        return SFSObject().apply {
            putInt("id", id)
            putInt("reward", reward)
        }
    }
}