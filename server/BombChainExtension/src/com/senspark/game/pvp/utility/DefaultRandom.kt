package com.senspark.game.pvp.utility

import kotlin.random.Random

class DefaultRandom(seed: Long) : IRandom {
    private val _random = Random(seed)

    override fun randomInt(minInclusive: Int, maxExclusive: Int): Int {
        return _random.nextInt(minInclusive, maxExclusive)
    }

    override fun randomFloat(minInclusive: Float, maxExclusive: Float): Float {
        return minInclusive + _random.nextFloat() * (maxExclusive - minInclusive)
    }
}