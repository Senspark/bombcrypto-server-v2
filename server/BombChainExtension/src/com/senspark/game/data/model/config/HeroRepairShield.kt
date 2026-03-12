package com.senspark.game.data.model.config

import java.sql.ResultSet

class HeroRepairShield(
    val rarity: Int,
    val shieldLevel: Int,
    val price: Float,
    val priceRock: Float
) {
    companion object {
        fun fromResultSet(rs: ResultSet): HeroRepairShield {
            return HeroRepairShield(
                rs.getInt("rarity"),
                rs.getInt("shield_level"),
                rs.getDouble("price").toFloat(),
                rs.getDouble("price_rock").toFloat()
            )
        }
    }
}