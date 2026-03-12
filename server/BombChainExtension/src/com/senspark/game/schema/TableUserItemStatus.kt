package com.senspark.game.schema

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object TableUserItemStatus : Table("user_item_status") {
    val uid = integer("uid")
    val id = integer("id")
    val itemId = integer("item_id")
    val status: Column<Int> = integer("status")
    val expiryDate: Column<Instant?> = timestamp("expiry_date").nullable()

    const val STATUS_NORMAL = 0
    const val STATUS_LOCK = 1
    const val STATUS_SELLING = 2
}