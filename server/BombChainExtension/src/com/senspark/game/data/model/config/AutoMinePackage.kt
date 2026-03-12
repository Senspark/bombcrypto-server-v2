package com.senspark.game.data.model.config

import com.google.gson.JsonObject
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import java.sql.ResultSet

class AutoMinePackage(
    val name: String,
    private val numDays: Int,
    val minPrice: Double,
    private val pricePercent: Double
) {
    companion object {
        fun fromResultSet(rs: ResultSet): AutoMinePackage {
            return AutoMinePackage(
                rs.getString("package_name"),
                rs.getInt("num_day"),
                rs.getDouble("min_price"),
                rs.getDouble("price_percent"),
            )
        }
    }

    fun toJsonObject(): JsonObject {
        val obj = JsonObject()
        obj.addProperty("package", name)
        obj.addProperty("num_days", numDays)
        obj.addProperty("price_percent", pricePercent)
        obj.addProperty("min_price", minPrice)
        return obj
    }

    fun toSFSObject(): ISFSObject {
        val obj = SFSObject()
        obj.putUtfString("package", name)
        obj.putInt("num_days", numDays)
        obj.putDouble("price_percent", pricePercent)
        obj.putDouble("min_price", minPrice)
        return obj
    }
}