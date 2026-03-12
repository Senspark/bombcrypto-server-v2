package com.senspark.game.data.model

import com.senspark.game.utils.deserialize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.sql.ResultSet

@Serializable
class PvpRankingReward(
    @SerialName("rank_min")
    val rankMin: Int,
    @SerialName("rank_max")
    val rankMax: Int,
    val reward: PvpReward?
) {
    companion object {
        fun fromResultSet(rs: ResultSet): PvpRankingReward {
            return PvpRankingReward(
                rs.getInt("rank_min"),
                rs.getInt("rank_max"),
                deserialize(rs.getString("reward"))
            )
        }
    }
}