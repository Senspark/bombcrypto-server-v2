package com.senspark.common.constant

enum class PvPItemType(val value: Int, val isChest: Boolean = false) {
    Default(-1),
    BombUp(0),
    FireUp(1),
    Boots(2),
    Shield(3),
    Kick(4),
    ShardNft(5),
    SkullHead(6),
    Chest(7, true),
    GoldX1(8),
    GoldX5(9),
    SilverChest(10, true),
    GoldChest(11, true),
    PlatinumChest(12, true);

    companion object {
        private val types = values().associateBy { it.value }
        fun fromInt(value: Int) = types[value] ?: throw Exception("Could not find pvp item type: $value")
    }
}