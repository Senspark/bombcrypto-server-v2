package com.senspark.game.declare.customEnum

enum class SubscriptionState(val value: String) {
    INVALID("invalid"),
    PENDING("pending"),
    ACTIVE("active"),
    EXPIRED("expired");

    companion object {
        private val types = SubscriptionState.values().associateBy { it.value }
        fun fromValue(value: String) = types[value] ?: throw Exception("Could not find action name: $value")
    }
}