package com.senspark.game.schema

import org.jetbrains.exposed.sql.Table

object TableNewUserGift : Table("config_new_user_gift") {
    val itemId = integer("item_id")
    val quantity = integer("quantity")
    val active = bool("active")
    val expirationAfter = long("expiration_after").nullable()
    val step = integer("step")
} 