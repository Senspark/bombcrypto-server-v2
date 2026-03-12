package com.senspark.game.pvp.manager

import com.senspark.game.pvp.entity.BombReason
import com.senspark.game.pvp.entity.Direction
import com.senspark.game.pvp.entity.IBomb
import com.senspark.game.pvp.entity.IBombState

interface IBombListener {
    fun onAdded(bomb: IBomb, reason: BombReason)
    fun onRemoved(bomb: IBomb, reason: BombReason)
    fun onExploded(bomb: IBomb, ranges: Map<Direction, Int>)
    fun onDamaged(x: Int, y: Int, amount: Int)
}

interface IBombManagerState {
    val bombCounter: Int
    val bombs: Map<Int, IBombState>
    fun apply(state: IBombManagerState): IBombManagerState
    fun encode(): List<Long>
}

interface IBombManager : IUpdater {
    /** Gets the current state. */
    val state: IBombManagerState

    /** Applies the specified slot. */
    fun applyState(state: IBombManagerState)

    /**
     * Gets owned bombs for the specified hero slot.
     * @param slot The desired slot.
     */
    fun getBombs(slot: Int): List<IBomb>

    /**
     * Gets the bomb at the specified position.
     * @param x Horizontal position.
     * @param y Vertical position.
     */
    fun getBomb(x: Int, y: Int): IBomb?

    fun plantBomb(state: IBombState): IBomb

    /**
     * Adds the specified bomb.
     * @param bomb The desired bomb.
     */
    fun addBomb(bomb: IBomb)

    /**
     * Removes the specified bomb.
     * @param bomb The desired bomb.
     */
    fun removeBomb(bomb: IBomb)

    /**
     * Explodes the specified bomb.
     * @param bomb The desired bomb.
     */
    fun explodeBomb(bomb: IBomb)

    /**
     * Gets explode ranges.
     * @param bomb The desired bomb.
     */
    fun getExplodeRanges(bomb: IBomb): Map<Direction, Int>

    /**
     * Throws the specified bomb.
     * @param bomb The desired bomb.
     * @param direction Throw direction.
     * @param distance Throw distance (blocks).
     * @param duration In milliseconds.
     */
    fun throwBomb(bomb: IBomb, direction: Direction, distance: Int, duration: Int)
}