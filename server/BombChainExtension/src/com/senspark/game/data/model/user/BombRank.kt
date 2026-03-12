package com.senspark.game.data.model.user

import com.senspark.common.data.IBombRank
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import java.sql.ResultSet

class BombRank(
    override val bombRank: Int,
    override val startPoint: Int,
    override val endPoint: Int,
    override val name: String,
    override val winPoint: Int,
    override val loosePoint: Int,
    override val minMatches: Int,
    override val decayPoint: Int
) : IBombRank {
    companion object {
        fun fromResultSet(rs: ResultSet): BombRank {
            return BombRank(
                rs.getInt("bomb_rank"),
                rs.getInt("start_point"),
                rs.getInt("end_point"),
                rs.getString("name"),
                rs.getInt("win_point"),
                rs.getInt("loose_point"),
                rs.getInt("min_matches"),
                rs.getInt("decay_point"),
            )
        }

        fun toSfsObject(r: IBombRank): ISFSObject {
            val result = SFSObject()
            result.putInt("bomb_rank", r.bombRank)
            result.putInt("start_point", r.startPoint)
            result.putInt("end_point", r.endPoint)
            result.putUtfString("name", r.name)
            return result
        }
    }

    fun toSfsObject(): ISFSObject {
        return toSfsObject(this)
    }
}