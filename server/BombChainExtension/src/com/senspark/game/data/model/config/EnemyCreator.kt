package com.senspark.game.data.model.config

import java.sql.ResultSet

class EnemyCreator(
    val id: Int,
    val health: Float,
    val damage: Int,
    val speedMove: Float,
    val follow: Int,
    val throughBrick: Int,
    val goldRewardFirstTime: Int,
    val goldRewardOtherTime: Int,
    val range: Int
) {
    companion object {
        fun fromResulSet(rs: ResultSet): EnemyCreator {
            return EnemyCreator(
                id = rs.getInt("entity_id"),
                health = rs.getInt("health").toFloat(),
                damage = rs.getInt("damage"),
                speedMove = rs.getInt("speed_move").toFloat(),
                follow = rs.getInt("follow"),
                throughBrick = rs.getInt("through"),
                goldRewardFirstTime = rs.getInt("gold_reward_first_time"),
                goldRewardOtherTime = rs.getInt("gold_reward_other_time"),
                range = rs.getInt("range"),
            )
        }
    }
}