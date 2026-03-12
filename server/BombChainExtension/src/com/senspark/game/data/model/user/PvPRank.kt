package com.senspark.game.data.model.user

import com.senspark.common.pvp.IRankManager
import com.senspark.game.user.UserPoint
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import java.sql.ResultSet


class PvPRank(
    var name: String?,
    val uid: Int,
    var rank: Int,
    val point: UserPoint,
    var matchCount: Int,
    var winMatch: Int,
    var avatar: Int?,
    private val _rankManager: IRankManager,
) {
    val bombRank get() = _rankManager.getRank(point.value)

    companion object {
        fun fromResultSet(rs: ResultSet, rankManager: IRankManager): PvPRank {
            return PvPRank(
                rs.getString("name"),
                rs.getInt("uid"),
                rs.getInt("rank") ?: 0,
                UserPoint(rs.getInt("point")),
                rs.getInt("total_match"),
                rs.getInt("win_match"),
                -1,//Avatar sẽ đc update sau ở PvpRankingManager
                rankManager
            )
        }
    }

    fun toSFSObject(): ISFSObject {
        val result = SFSObject()
        result.putInt("user_id", uid)
        result.putInt("point", point.value)
        result.putInt("rank_number", rank)
        result.putInt("total_match", matchCount)
        result.putInt("win_match", winMatch)
        result.putInt("bomb_rank", bombRank)
        result.putInt("avatar", avatar ?: -1)
        if (name == null) result.putNull("name") else result.putUtfString("name", name)
        return result
    }

    /**
     * update point
     * @return pair of oldPoint,newPoint
     */
    fun update(pointBonus: Int, match: Int, win: Int) {
        val oldPoint = point.value
        matchCount += match
        winMatch += win
        point.add(pointBonus)
    }
}