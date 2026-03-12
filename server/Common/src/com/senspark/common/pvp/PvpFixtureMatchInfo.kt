package com.senspark.common.pvp

import kotlinx.serialization.*
import java.sql.ResultSet

@Serializable
class PvpFixtureMatchUserInfo(
    @SerialName("score") override val score: Int,
    @SerialName("user_id") override val userId: Int,
    @SerialName("username") override val username: String,
    @SerialName("display_name") override val displayName: String,
    @SerialName("rank") override val rank: Int,
) : IPvpFixtureMatchUserInfo

@Serializable
class PvpFixtureMatchInfo(
    @SerialName("id") override val id: String,
    @SerialName("status") override val status: PvpFixtureMatchStatus,
    @SerialName("find_begin_timestamp") override val findBeginTimestamp: Long,
    @SerialName("find_end_timestamp") override val findEndTimestamp: Long,
    @SerialName("finish_timestamp") override val finishTimestamp: Long,
    @SerialName("mode") override val mode: PvpMode,
    @SerialName("info") override val info: List<IPvpFixtureMatchUserInfo>,
) : IPvpFixtureMatchInfo {
    companion object {
        fun fromResultSet(rs: ResultSet, rankManager: IRankManager): IPvpFixtureMatchInfo {
            return PvpFixtureMatchInfo(
                id = rs.getString("id"),
                status = when (rs.getString("status")) {
                    "PENDING" -> PvpFixtureMatchStatus.Pending
                    "ABORTED" -> PvpFixtureMatchStatus.Aborted
                    "COMPLETED" -> PvpFixtureMatchStatus.Completed
                    else -> {
                        require(false) { "Invalid status" }
                        PvpFixtureMatchStatus.Pending
                    }
                },
                findBeginTimestamp = rs.getLong("find_begin_timestamp"),
                findEndTimestamp = rs.getLong("find_end_timestamp"),
                finishTimestamp = rs.getLong("finish_timestamp"),
                mode = PvpMode.fromValue(rs.getInt("mode")),
                (1..2).map {
                    val username = rs.getString("username_$it")
                    PvpFixtureMatchUserInfo(
                        score = rs.getInt("user_${it}_score"),
                        userId = rs.getInt("user_id_$it"),
                        username = username,
                        displayName = rs.getString("display_name_$it") ?: username,
                        rank = rankManager.getRank(rs.getInt("rank_point_$it")),
                    )
                }
            )
        }
    }
}