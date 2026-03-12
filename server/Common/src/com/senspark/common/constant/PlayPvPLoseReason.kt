package com.senspark.common.constant

enum class PlayPvPLoseReason(val value: String) {
    Bomb("bomb"),
    Block("block_drop"),
    Hero("hero"),
    Quit("quit"),
    Null("");

    companion object {
        private val types = PlayPvPLoseReason.values().associateBy { it.value }
        fun fromString(value: String) = types[value] ?: throw Exception("Could not find play pvp lose reason: $value")
    }
}