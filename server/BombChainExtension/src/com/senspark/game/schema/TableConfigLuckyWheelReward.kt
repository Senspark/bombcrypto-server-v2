package com.senspark.game.schema

import org.jetbrains.exposed.sql.Table

object TableConfigLuckyWheelReward : Table("config_lucky_wheel_reward") {
    val code = varchar("code", 30)
    val itemId = integer("item_id").nullable()
    val quantity = integer("quantity").nullable()
    val active = bool("active")
    val sort = integer("sort")
    val weight = integer("weight")
}