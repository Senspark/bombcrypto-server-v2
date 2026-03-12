package com.senspark.game.data.model.config

import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import java.sql.ResultSet

class TonReferralTasksConfig(
    val id: Int,
    val name: String,
    val reward: Float
) {

    fun toSFSObject(): ISFSObject {
        return SFSObject().apply {
            putInt("id", id)
            putUtfString("name", name)
            putFloat("reward", reward)
        }
    }

    companion object {
        fun fromResultSet(rs: ResultSet): TonReferralTasksConfig {
            return TonReferralTasksConfig(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getFloat("reward_percent")
            )
        }
    }
}