package com.senspark.game.data.model.user

import com.senspark.game.declare.EnumConstants
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import java.sql.ResultSet

class CoinRank(
    var uid: Int,
    var name: String,
    var coinTotal: Double,
    var coinCurrentSeason: Double,
    var network: EnumConstants.DataType,
) {
    companion object {
        fun fromResultSet(rs: ResultSet): CoinRank {
            return CoinRank(
                rs.getInt("uid"),
                rs.getString("name"),
                rs.getDouble("coin_total"),
                rs.getDouble("coin_current_season"),
                EnumConstants.DataType.valueOf(rs.getString("network"))
            )
        }
    }

    fun toSFSObject(isAllSeason: Boolean): ISFSObject {
        val result = SFSObject()
        result.putInt("user_id", uid)
        result.putUtfString("name", name)
        if (isAllSeason) {
            result.putDouble("coin", coinTotal)
        } else {
            result.putDouble("coin", coinCurrentSeason)
        }
        return result
    }
}