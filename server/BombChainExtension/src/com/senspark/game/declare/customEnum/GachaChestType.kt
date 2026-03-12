package com.senspark.game.declare.customEnum

enum class GachaChestType(val value: Int) {
    BRONZE(1),
    SILVER(2),
    GOLD(3),
    PLATINUM(4),
    RUBY(5),
    DIAMOND(6),
    ORDINARY(7),
    PREMIUM(8),
    COSMIC(9),
    GALACTIC(10),
    ORION(11),
    DAILY(12);

    companion object {
        private val types = GachaChestType.values().associateBy { it.value }
        fun fromValue(value: Int): GachaChestType = types[value] ?: throw Exception("Could not find type: $value")
    }
}