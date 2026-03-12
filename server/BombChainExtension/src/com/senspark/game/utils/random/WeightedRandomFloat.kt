package com.senspark.game.utils.random

import com.senspark.game.data.model.config.IHasWeightEntity
import kotlin.random.Random

class WeightedRandomFloat<T : IHasWeightEntity>(private val _items: List<T>) : IWeightedRandom<T> {
    private val _sum = _items.map { it.weight }.sum()

    override fun randomItem(): T {
        var r = Random.nextFloat() * _sum
        for (i in _items.indices) {
            val item = _items[i]
            if (r < item.weight) {
                return item
            }
            r -= item.weight
        }
        throw Exception("Could not random")
    }
}
