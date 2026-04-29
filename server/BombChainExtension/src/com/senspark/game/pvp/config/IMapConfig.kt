package com.senspark.game.pvp.config

interface IMapConfig {
    /** Amount of time for a match (ms). */
    val playTime: Int

    val tilesetList: List<Int>

    val blockDensity: Float

    val itemDensity: Float

    /** Config for falling block. */
    val fallingBlockPatternList: List<FallingBlockPattern>

    /** Amount of time until a bomb explodes (ms). */
    val explodeDuration: Int

    /** Maximum number of players allowed on this map. */
    val maxPlayers: Int

    /** Unique identifier for the map layout pattern. */
    val mapPatternId: String
}