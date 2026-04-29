package com.senspark.game.pvp.config

import com.senspark.common.pvp.PvpMode

class DynamicMapConfig(private val mode: PvpMode) : IMapConfig {
    override val playTime: Int
        get() = when (mode) {
            PvpMode.FFA_2, PvpMode.FFA_2_B3, PvpMode.FFA_2_B5, PvpMode.FFA_2_B7 -> 120000
            PvpMode.Team_2v2, PvpMode.Team_3v3 -> 150000
            PvpMode.BATTLE_ROYALE -> 180000
            else -> 120000
        }

    override val tilesetList = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
    override val blockDensity = 0.5f
    override val itemDensity = 0.55f
    override val fallingBlockPatternList = listOf(
        FallingBlockPattern.TopLeftCw,
        FallingBlockPattern.TopLeftCcw,
        FallingBlockPattern.BottomRightCw,
        FallingBlockPattern.BottomRightCcw,
        FallingBlockPattern.TopLeftDualCw,
        FallingBlockPattern.TopLeftDualCcw,
    )
    override val explodeDuration = 3000

    override val maxPlayers: Int
        get() = when (mode) {
            PvpMode.FFA_2, PvpMode.FFA_2_B3, PvpMode.FFA_2_B5, PvpMode.FFA_2_B7 -> 2
            PvpMode.FFA_3 -> 3
            PvpMode.FFA_4 -> 4
            PvpMode.Team_2v2 -> 4
            PvpMode.Team_3v3 -> 6
            PvpMode.BATTLE_ROYALE -> 6
            else -> 2
        }

    override val mapPatternId: String
        get() = when (mode) {
            PvpMode.FFA_2, PvpMode.FFA_2_B3, PvpMode.FFA_2_B5, PvpMode.FFA_2_B7 -> "SMALL_1V1"
            PvpMode.FFA_3, PvpMode.FFA_4 -> "MEDIUM_TEAM"
            PvpMode.Team_2v2, PvpMode.Team_3v3 -> "MEDIUM_TEAM"
            PvpMode.BATTLE_ROYALE -> "LARGE_BR"
            else -> "SMALL_1V1"
        }
}
