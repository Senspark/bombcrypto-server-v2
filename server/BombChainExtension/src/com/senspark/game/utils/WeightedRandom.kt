package com.senspark.game.utils

import kotlin.random.Random

class WeightedRandom(private val _weights: List<Float>) {
    private val _sum = _weights.sum()

    fun random(random: Random): Int {
        if (_weights.size == 1) {
            return 0
        }
        var r = random.nextFloat() * _sum
        for (i in _weights.indices) {
            if (r < _weights[i]) {
                return i
            }
            r -= _weights[i]
        }
        throw Exception("Could not random")
    }
}