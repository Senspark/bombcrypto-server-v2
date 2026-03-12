package com.senspark.game.data.model.config

import java.sql.ResultSet

class ResetShieldBomber(
    private var rare: Int, private var finalDamage: Int
) {
    fun getRare(): Int {
        return rare
    }

    fun getFinalDamage(): Int {
        return finalDamage
    }

    companion object {
        fun fromResulSet(rs: ResultSet): ResetShieldBomber {
            return ResetShieldBomber(rs.getInt("rare"), rs.getInt("final_damage"))
        }
    }
}