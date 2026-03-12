package com.senspark.game.schema

import org.jetbrains.exposed.sql.Table

object TableSkinChestDropRate : Table("config_skin_chest_drop_rate") {
    val value = varchar("value", 50)
}