package com.senspark.game.schema

import org.jetbrains.exposed.sql.Table

object TableUserGachaChest : Table("user_gacha_chest") {
    val chestId = integer("chest_id")
    val userId = integer("uid")
    val chestType = integer("chest_type")
    val openTime = long("open_time")
    val isDeleted = integer("deleted")
    override val primaryKey = PrimaryKey(chestId)
}