package com.senspark.game.schema

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object TableUserBooster : Table("user_booster") {
    val id: Column<Int> = integer("id").autoIncrement()
    val uid = integer("uid")
    val type: Column<Int> = integer("type")
    val itemId: Column<Int> = integer("item_id")
    val status: Column<Int> = integer("status")
    val create: Column<Instant> = timestamp("create_date")
}