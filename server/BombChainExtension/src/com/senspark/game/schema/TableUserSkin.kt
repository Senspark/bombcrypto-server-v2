package com.senspark.game.schema

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object TableUserSkin : Table("user_skin") {
    val id = integer("id").autoIncrement()
    val uid = integer("uid")
    val type = integer("type")
    val itemId = integer("item_id")
    val status = integer("status")
    val createDate = timestamp("create_date")
    val expirationAfter = long("expiration_after").nullable()

    const val STATUS_INACTIVE = 0
    const val STATUS_ACTIVE = 1
}