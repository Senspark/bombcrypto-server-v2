package com.senspark.game.pvp.config

class ConstantMapConfig : IMapConfig {
    override val playTime = 120000

    /**
     * Map tileset.
     * Normal tileset: 0 -> 8.
     * Tet tileset: 100 -> 107.
     */
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
    override val maxPlayers = 2
    override val mapPatternId = "SMALL_1V1"
}