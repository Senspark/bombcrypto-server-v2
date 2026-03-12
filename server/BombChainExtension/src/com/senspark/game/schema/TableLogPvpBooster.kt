package com.senspark.game.schema

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object TableLogPvpBooster : Table("log_pvp_booster") {
    val date = timestamp("date")
    val uid = integer("uid")
    val itemId = integer("item_id")
    val type = varchar("type", 4)
    val boosterName = varchar("booster_name", 30)
    val feeAmount = float("fee_amount")
}