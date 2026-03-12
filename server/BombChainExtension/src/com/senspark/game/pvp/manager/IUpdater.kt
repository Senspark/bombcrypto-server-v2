package com.senspark.game.pvp.manager

/**
 * Refers an object that can "step".
 */
interface IUpdater {
    /** Steps by the specified duration. */
    fun step(delta: Int)
}