package com.senspark.game.constant

enum class Booster(
    val value: Int,
    val coolDown: Int = 0,
    val timeEffect: Int = 0,
    val gameMode: GameMode = GameMode.ALL
) {
    Key(18, 20, 5),
    Shield(19, 20, 5),
    RankGuardian(20, gameMode = GameMode.PVP),
    FullRankGuardian(21, gameMode = GameMode.PVP),
    ConquestCard(22, gameMode = GameMode.PVP),
    FullConquestCard(23, gameMode = GameMode.PVP),
    BombPlusOne(26),
    SpeedPlusOne(27),
    RangePlusOne(28);

    companion object {
        private val types = Booster.values().associateBy { it.value }
        fun fromValue(value: Int) = types[value] ?: throw Exception("Could not find action name: $value")
    }
}