package com.senspark.game.declare.customEnum

enum class IapStore(val storeName: String, val value: Int) {
    GOOGLE_PLAY("google-play", 0),
    APPLE_STORE("apple", 1);

    companion object {
        private val types = IapStore.values().associateBy { it.value }
        fun fromValue(value: Int): IapStore = types[value] ?: throw Exception("Could not find IapStore: $value")
    }
}