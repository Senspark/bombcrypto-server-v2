package com.senspark.game.pvp.entity

enum class BombReason {
    Null,

    /** Add reasons. */
    /** Planted by hero. */
    Planted,

    /** Remove reasons. */
    /** Explode itself. */
    Exploded,

    /** Removed by falling blocks. */
    Removed,

    Throw,

    /** Planted by skull effect. */
    PlantedBySkull,
}

interface IBomb : IEntity {
    val state: IBombState

    /** Owner ID, in range [0, playerCount). */
    val slot: Int

    /** Bomb ID, unique. */
    val id: Int

    val reason: BombReason

    /** Horizontal position. */
    val x: Float

    /** Vertical position. */
    val y: Float

    /** Bomb range, in range [1, +). */
    val range: Int

    /** Bomb damage, in range [1, +). */
    val damage: Int

    val piercing: Boolean

    /** When the bomb is planted in milliseconds. */
    val plantTimestamp: Int

    fun applyState(state: IBombState)
    fun kill(reason: BombReason)
}