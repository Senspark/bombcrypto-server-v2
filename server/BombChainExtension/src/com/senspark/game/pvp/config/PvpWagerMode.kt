package com.senspark.game.pvp.config

enum class PvpWagerMode(val value: Int) {
    FREE(0),
    WAGERED(1);

    companion object {
        fun from(value: Int) = entries.firstOrNull { it.value == value } ?: FREE
    }
}
