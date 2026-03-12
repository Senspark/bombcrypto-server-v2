package com.senspark.game.data.model.config

import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import java.sql.ResultSet

class CoinLeaderboardConfig(
    val rank: Int,
    val name: String,
    val upRankUserPoint: Int,
    val upRankClubPoint: Long
) {
    companion object {
        fun fromResultSet(rs: ResultSet): CoinLeaderboardConfig {
            return CoinLeaderboardConfig(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("up_rank_point_user"),
                rs.getLong("up_rank_point_club"),
            )
        }
    }
    
    fun toSFSObject(): ISFSObject {
        return SFSObject().apply { 
            putInt("rank", rank)
            putUtfString("name", name)
            putInt("up_rank_point_user", upRankUserPoint)
            putLong("up_rank_point_club", upRankClubPoint)
        }
    }
}