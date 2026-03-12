package com.senspark.game.schema

import org.jetbrains.exposed.sql.Table

object TableScheduleStatus : Table("schedule_status") {
    val schedule = varchar("schedule", 20)
    val status = integer("status")
    val season = integer("season")
}