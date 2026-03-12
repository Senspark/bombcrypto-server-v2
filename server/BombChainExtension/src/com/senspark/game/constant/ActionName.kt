package com.senspark.game.constant

enum class ActionName(val value: Int) {
    Buy(0), Sell(1);

    companion object {
        private val types = ActionName.values().associateBy { it.value }
        fun fromValue(value: Int) = types[value] ?: throw Exception("Could not find action name: $value")
    }
}