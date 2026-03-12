package com.senspark.game.schema

import org.jetbrains.exposed.sql.Table

object TableConfigDropRate : Table("config_drop_rate") {
    val name = varchar("name", 20)
    val dropRate = varchar("drop_rate", 255)
}