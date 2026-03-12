package com.senspark.game.data.model.config

import java.sql.ResultSet
import java.time.Instant

class Season(
    val id: Int,
    val startDate: Long,
    val endDate: Long,
    val nextSeasonStartDate: Long?
) {
    companion object {
        fun fromResultSet(rs: ResultSet): Season {
            return Season(
                rs.getInt("id"),
                rs.getLong("start_date"),
                rs.getLong("end_date"),
                rs.getLong("next_season_start_date"),
            )
        }
    }

    /**
     * Mùa giả đà kết thúc, nhưng chưa bắt đầu mùa giải mới
     */
    val seasonEnded: Boolean
        get() {
            val now = Instant.now().toEpochMilli()
            return now > endDate
        }

    /**
     * thời gian mùa giải mới đã bắt đầu
     */
    val seasonClosed: Boolean
        get() {
            val now = Instant.now().toEpochMilli()
            nextSeasonStartDate.let {
                if (it != null) {
                    return now > it
                } else {
                    return seasonEnded
                }
            }
        }

    fun getRemainingTime(): Long {
        val currentTime = Instant.now().toEpochMilli()
        return nextSeasonStartDate.let {
            if (it == null) {
                0
            } else {
                if (seasonEnded) {
                    (currentTime - it) / 1000 / 60
                } else {
                    (endDate - currentTime) / 1000 / 60
                }
            }
        }
    }
}