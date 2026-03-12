package com.senspark.game.schema

import org.jetbrains.exposed.sql.Table

object TableGrindHero : Table("config_grind_hero") {
    val itemKind = varchar("item_kind", 30)
    val dropItems = text("drop_items")
    val price = integer("price")
} 