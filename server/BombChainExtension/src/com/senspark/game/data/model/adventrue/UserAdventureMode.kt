package com.senspark.game.data.model.adventrue

import java.sql.ResultSet

class UserAdventureMode(
    var currentStage: Int = 1,
    var maxStage: Int = 0,
    var currentLevel: Int = 1,
    var maxLevel: Int = 0,
    var heroId: Int = 0
) {
    companion object {
        fun fromResulSet(rs: ResultSet): UserAdventureMode {
            return UserAdventureMode(
                currentLevel = rs.getInt("current_level"),
                maxLevel = rs.getInt("max_level"),
                currentStage = rs.getInt("current_stage"),
                maxStage = rs.getInt("max_stage"),
                heroId = rs.getInt("hero_id")
            )
        }
    }
}