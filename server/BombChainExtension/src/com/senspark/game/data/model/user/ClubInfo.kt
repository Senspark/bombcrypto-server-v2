package com.senspark.game.data.model.user

import java.sql.ResultSet

class ClubInfo(
    val id: Int,
    val name: String,
    val link: String? = null,
    var pointTotal: Double,
    var pointCurrentSeason: Double,
    val avatarName: String? = null,
    val telegramId: Long? = null
) {
    companion object {
        fun fromResultSet(rs: ResultSet): ClubInfo {
            return ClubInfo(
                rs.getInt("club_id"),
                rs.getString("name"),
                rs.getString("link"),
                rs.getDouble("point_total"),
                rs.getDouble("point_current_season"),
                rs.getString("avatar_name"),
                rs.getLong("id_telegram")
            )
        }
    }
}
