package com.senspark.game.data.model.config

import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import java.sql.ResultSet

class HouseRentPackage(
    val rarity: Int,
    val numDays: Int,
    val price: Float
) {
    companion object {
        fun fromResultSet(rs: ResultSet): HouseRentPackage {
            return HouseRentPackage(
                rs.getInt("rarity"),
                rs.getInt("num_days"),
                rs.getFloat("price"),
            )
        }
    }

    fun toSFSObject(): ISFSObject {
        val obj = SFSObject()
        obj.putInt("rarity", rarity)
        obj.putInt("num_days", numDays)
        obj.putFloat("price", price)
        return obj
    }
}