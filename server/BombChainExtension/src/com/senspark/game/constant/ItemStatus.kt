package com.senspark.game.constant

enum class ItemStatus(val value: Int) {
    Normal(0),
    LockedOrEquipSkin(1),
    Sell(2),
    Delete(3);

    companion object {
        private val types = ItemStatus.values().associateBy { it.value }
        fun fromValue(value: Int) = types[value] ?: throw Exception("Could not find action name: $value")
    }
}