package com.senspark.game.schema

import org.jetbrains.exposed.sql.Table

object TableBuyGemTransaction : Table("user_buy_gem_transaction") {
    val billToken = varchar("bill_token", 255)
    val userId = integer("uid")
    val productId = varchar("product_id", 255)
    override val primaryKey = PrimaryKey(billToken)
}