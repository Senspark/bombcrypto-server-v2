package com.senspark.game.data.model.config

import com.google.gson.JsonObject
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.EnumConstants.DataType
import java.sql.ResultSet

class RockPackage(
    val _name: String,
    private val _rockAmount: Int,
    private val _senPrice: Double,
    private val _bcoinPrice: Double,
) {
    companion object {
        fun fromResultSet(rs: ResultSet): RockPackage {
            return RockPackage(
                rs.getString("pack_name"),
                rs.getInt("rock_amount"),
                rs.getDouble("sen_price"),
                rs.getDouble("bcoin_price")
            )
        }
    }

    fun toJsonObject(): JsonObject {
        val obj = JsonObject()
        obj.addProperty("package", _name)
        obj.addProperty("rock_amount", _rockAmount)
        obj.addProperty("sen_price", _senPrice)
        obj.addProperty("bcoin_price", _bcoinPrice)
        return obj
    }

    fun getRockAmount(): Int {
        return _rockAmount
    }

    fun getSenPrice(): Double {
        return _senPrice
    }

    fun getBcoinPrice(): Double {
        return _bcoinPrice
    }

    fun getName(): String {
        return _name
    }
}