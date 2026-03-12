package com.senspark.game.constant

enum class DropRate(val value: Int) {
    PVP_ITEM(0);

    companion object {
        private val types = DropRate.values().associateBy { it.value }
        fun fromValue(value: Int) = types[value] ?: throw Exception("Could not find action name: $value")
    }
}