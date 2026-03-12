package com.senspark.game.schema

import org.jetbrains.exposed.sql.Table

object TableUser : Table("user") {
    val userId = integer("id_user").autoIncrement()
}