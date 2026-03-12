package com.senspark.game.pvp.entity

interface IBombState : IEntityState {
    val slot: Int

    /** Alive/dead reason. */
    val reason: BombReason

    /** Horizontal position. */
    val x: Float

    /** Vertical position. */
    val y: Float

    val range: Int
    val damage: Int
    val piercing: Boolean
    val explodeDuration: Int
    val explodeRanges: Map<Direction, Int>
    val plantTimestamp: Int

    fun encode(): List<Long>
}