package com.senspark.game.pvp.config

enum class PvpWagerTier(val id: Int, val amount: Double) {
    NONE(0, 0.0),
    T1(1, 1.0),
    T2(2, 5.0),
    T3(3, 10.0),
    T4(4, 25.0),
    T5(5, 50.0),
    T6(6, 100.0),
    T7(7, 1000.0),
    T8(8, 5000.0),
    T9(9, 10000.0),
    T10(10, 25000.0),
    T11(11, 50000.0),
    T12(12, 100000.0);

    companion object {
        fun from(id: Int) = values().firstOrNull { it.id == id } ?: NONE
        fun fromAmount(amount: Double) = values().firstOrNull { it.amount == amount } ?: NONE
    }
}
