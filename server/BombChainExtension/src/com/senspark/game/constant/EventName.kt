package com.senspark.game.constant

enum class EventName(val value: String) {
    SkinChest("NFT_CHEST_EVENT");

    companion object {
        private val types = EventName.values().associateBy { it.value }
        fun fromValue(value: String) = types[value] ?: throw Exception("Could not find event name: $value")
    }
}