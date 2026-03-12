package com.senspark.game.pvp.strategy.position

interface IPositionStrategy {
    fun getPosition(timestamp: Int): Pair<Float, Float>
    fun addPosition(timestamp: Int, x: Float, y: Float)
}