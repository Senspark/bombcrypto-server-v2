package com.senspark.game.data.model.config

import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import java.sql.ResultSet

class RewardLevelConfig(
    val numUsers: Int,
    val amountReward: Map<BLOCK_REWARD_TYPE, Double>
) {
    companion object {
        fun fromResultSet(rs: ResultSet): RewardLevelConfig {
            val numUsers = rs.getInt("num_users")
            val amountReward = mapOf(
                BLOCK_REWARD_TYPE.BCOIN to rs.getDouble("bcoin"),
                BLOCK_REWARD_TYPE.SENSPARK to rs.getDouble("sen"),
                BLOCK_REWARD_TYPE.COIN to rs.getDouble("coin")
            )
            return RewardLevelConfig(numUsers, amountReward)
        }
    }
}