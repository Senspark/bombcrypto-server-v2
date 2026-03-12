package com.senspark.game.pvp.utility

interface IRandom {
    fun randomInt(minInclusive: Int, maxExclusive: Int): Int
    fun randomFloat(minInclusive: Float, maxExclusive: Float): Float
}