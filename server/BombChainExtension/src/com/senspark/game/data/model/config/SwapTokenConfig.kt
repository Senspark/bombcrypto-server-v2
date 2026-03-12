package com.senspark.game.data.model.config

import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.EnumConstants.DataType
import java.sql.ResultSet

class SwapTokenConfig(
    val fromToken: BLOCK_REWARD_TYPE,
    val fromNetwork: DataType,
    val toToken: BLOCK_REWARD_TYPE,
    val toNetwork: DataType,
    val ratio: Double
) {
    companion object {
        fun fromResultSet(rs: ResultSet): SwapTokenConfig {
            return SwapTokenConfig(
                BLOCK_REWARD_TYPE.valueOf(rs.getString("from_type")),
                DataType.valueOf(rs.getString("from_network")),
                BLOCK_REWARD_TYPE.valueOf(rs.getString("to_type")),
                DataType.valueOf(rs.getString("to_network")),
                rs.getDouble("ratio")
            )
        }
    }
}