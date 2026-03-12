package com.senspark.game.constant

enum class OrderBy(val value: Int) {
    ModifyDateDesc(0),
    ModifyDateAsc(1),
    PriceDesc(2),
    PriceAsc(3),
    AmountAsc(4),
    AmountDesc(5);

    companion object {
        private val types = OrderBy.values().associateBy { it.value }
        fun fromValue(value: Int) = types[value] ?: throw Exception("Could not find action name: $value")
    }
}