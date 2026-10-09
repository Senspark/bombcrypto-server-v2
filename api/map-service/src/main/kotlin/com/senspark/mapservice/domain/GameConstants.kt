package com.senspark.mapservice.domain

/**
 * Ported from BombChainExtension's `com.senspark.game.declare.GameConstants`. These are fixed
 * gameplay constants (map bounds, block-type codes) that never change without a redeploy of both
 * sides, so they're compiled in here rather than sent over the wire.
 */
object GameConstants {
    const val MAP_MAX_COL: Int = 35
    const val MAP_MAX_ROW: Int = 17

    object BlockType {
        const val ROCK: Int = 0
        const val NORMAL: Int = 1
        const val JAIL: Int = 2
        const val WOODEN: Int = 3
        const val SILVER: Int = 4
        const val GOLDEN: Int = 5
        const val DIAMOND: Int = 6
        const val LEGEND: Int = 7
    }
}
