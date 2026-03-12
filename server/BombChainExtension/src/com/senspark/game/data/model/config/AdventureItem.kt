package com.senspark.game.data.model.config

import com.senspark.common.constant.PvPItemType
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.lib.utils.Util
import java.sql.ResultSet

class AdventureItem(
    val type: PvPItemType,
    val dropRate: Int,
    private val rewardMin: Int,
    private val rewardMax: Int,
    val rewardType: BLOCK_REWARD_TYPE?
) {
    val rewardValue: Int get() = Util.randInt(rewardMin, rewardMax)
    
    companion object {
        fun fromResultSet(rs: ResultSet): AdventureItem {
            return AdventureItem(
                PvPItemType.valueOf(rs.getString("type")),
                rs.getInt("drop_rate"),
                rs.getInt("reward_min"),
                rs.getInt("reward_max"),
                rs.getString("reward_type")?.let {
                    BLOCK_REWARD_TYPE.valueOf(it)
                })
        }
    }
}