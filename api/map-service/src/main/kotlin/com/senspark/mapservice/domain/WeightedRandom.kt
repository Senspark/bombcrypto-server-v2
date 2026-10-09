package com.senspark.mapservice.domain

import kotlin.random.Random

/** Verbatim port of BombChainExtension's `com.senspark.game.utils.WeightedRandom`. */
class WeightedRandom(private val weights: List<Float>) {
    private val sum = weights.sum()

    fun random(random: Random): Int {
        if (weights.size == 1) {
            return 0
        }
        var r = random.nextFloat() * sum
        for (i in weights.indices) {
            if (r < weights[i]) {
                return i
            }
            r -= weights[i]
        }
        throw IllegalStateException("Could not random")
    }
}
