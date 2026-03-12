package com.senspark.game.data.model.config

import com.senspark.game.declare.EnumConstants.StakeVipRewardType
import com.smartfoxserver.v2.entities.data.SFSObject
import java.sql.ResultSet

class StakeVipReward(
    val level: Int,
    val stakeAmount: Double,
    val rewardType: StakeVipRewardType,
    val type: String,
    val quantity: Int,
    val dates: Int,
) {
    companion object {
        fun fromResulSet(rs: ResultSet): StakeVipReward {
            return StakeVipReward(
                rs.getInt("level"),
                rs.getDouble("stake_amount"),
                StakeVipRewardType.valueOf(rs.getString("reward_type")),
                rs.getString("type"),
                rs.getInt("quantity"),
                rs.getInt("dates")
            )
        }
    }

    fun toSfsObject(): SFSObject {
        val sfsObject = SFSObject()
        sfsObject.putUtfString("rewardType", rewardType.name)
        sfsObject.putUtfString("type", type)
        sfsObject.putInt("quantity", quantity)
        sfsObject.putInt("dates", dates)
        return sfsObject
    }
}