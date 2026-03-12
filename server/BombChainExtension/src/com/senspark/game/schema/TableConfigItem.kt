package com.senspark.game.schema

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

object TableConfigItem : Table("config_item") {
    val id = integer("id")
    val type = integer("type")
    val name = varchar("name", 50)
    val descriptionEn = varchar("description_en", 100).nullable()
    val ability = varchar("ability", 100)
    val kind = varchar("kind", 30)
    val goldPrice7Days = integer("gold_price_7_days").nullable()
    val gemPrice7Days = integer("gem_price_7_days").nullable()
    val gemPrice30Days = integer("gem_price_30_days").nullable()
    val gemPrice = integer("gem_price").nullable()
    val goldPrice = integer("gold_price").nullable()
    val active = bool("active")
    val isSellable = bool("is_sellable")
    val tag = varchar("tag", 20).nullable()
    val isDefault = bool("is_default")
    val saleStartDate = date("sale_start_date").nullable()
    val saleEndDate = date("sale_end_date").nullable()
}