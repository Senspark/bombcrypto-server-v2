package com.senspark.game.schema

import org.jetbrains.exposed.sql.Table

object TableRemoteTokenConvert : Table("remote_token_convert") {
    val networkType = integer("network_type")
    val ratio = float("ratio")
    val tokenType = integer("token_type")
}