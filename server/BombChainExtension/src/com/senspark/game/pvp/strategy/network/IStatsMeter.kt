package com.senspark.game.pvp.strategy.network

interface IStatsMeter {
    /** Gets the current value. */
    val value: Int

    fun add(value: Int)
    fun remove(value: Int)
}