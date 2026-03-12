package com.senspark.game.schema

import org.jetbrains.exposed.sql.Table

object TableMysteryBox : Table("config_mystery_box_item") {
    val itemId = integer("item_id")
    val weight = integer("weight")
    val quantity = integer("quantity")
    val expirationAfter = long("expiration_after")
}