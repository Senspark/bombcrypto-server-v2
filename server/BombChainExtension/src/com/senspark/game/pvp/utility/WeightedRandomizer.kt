package com.senspark.game.pvp.utility

class WeightedRandomizer<T>(
    private val _items: List<T>,
    private val _weights: List<Float>,
) : IRandomizer<T> {
    private val _sum = _weights.sum()

    init {
        require(_items.size == _weights.size) { "Invalid item size" }
    }

    override fun random(random: IRandom): T {
        var r = random.randomFloat(0f, _sum)
        for (i in _weights.indices) {
            if (r < _weights[i]) {
                return _items[i]
            }
            r -= _weights[i]
        }
        throw Exception("Could not random")
    }
}