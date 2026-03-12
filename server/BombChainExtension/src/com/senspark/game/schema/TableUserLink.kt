package com.senspark.game.schema

import org.jetbrains.exposed.sql.Table

object TableUserLink : Table("user_link") {
    val userId = integer("user_id")
    val linkedUserId = integer("linked_user_id")
}