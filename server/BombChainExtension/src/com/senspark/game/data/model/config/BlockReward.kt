package com.senspark.game.data.model.config

import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.lib.utils.Util
import java.sql.ResultSet

class BlockReward(
    override val type: BLOCK_REWARD_TYPE,
    override val weight: Int,
    private val minValue: Float,
    private val maxValue: Float
) : IBlockReward {
    companion object {
        fun fromResultSet(rs: ResultSet): BlockReward {
            return BlockReward(
                BLOCK_REWARD_TYPE.valueOf(rs.getString("type")),
                rs.getInt("reward_weight"),
                rs.getDouble("min_reward").toFloat(),
                rs.getDouble("max_reward").toFloat()
            )
        }
    }

    override fun getValue(network: EnumConstants.DataType): Float {
        return Util.randFloat(minValue, maxValue)
    }

    override fun dump(): String {
        return "(typ=$type,wei=$weight,min=$minValue,max=$maxValue)"
    }
}