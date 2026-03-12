package com.senspark.common.constant

enum class PvPBlockType(val value: Int) {
    Null(0),
    Hard(1),
    Soft(2),
    NftChest(3);

    companion object {
        private val types = PvPBlockType.values().associateBy { it.value }
        fun fromValue(value: Int) = types[value] ?: throw Exception("Could not find pvp block type: $value")
    }
}