package com.senspark.game.pvp.config

enum class PvpGameMode(val value: Int, val maxPlayers: Int, val teamSize: Int) {
    DUEL_1V1(1, 2, 1),
    TEAM_2V2(2, 4, 2),
    TEAM_3V3(3, 6, 3),
    BATTLE_ROYALE_6P(4, 6, 1);

    companion object {
        fun from(value: Int) = entries.firstOrNull { it.value == value } ?: DUEL_1V1
    }
}
