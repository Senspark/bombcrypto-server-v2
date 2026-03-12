package com.senspark.game.data.model.config

import java.sql.ResultSet

class BlockConfig(val id: Int, val hp: Int, val rewardId: Int) {
    companion object {
        fun fromResulSet(rs: ResultSet): BlockConfig {
            return BlockConfig(
                rs.getInt("id"),
                rs.getInt("hp"),
                rs.getInt("id_reward")
            )
        }
    }
}