package com.senspark.game.pvp.strategy.network

class MedianStatsMeter : IStatsMeter {
    private var _dirty = false
    private val _values = mutableListOf<Int>()

    override val value: Int
        get() {
            if (_dirty) {
                _dirty = false
                _values.sort()
            }
            return if (_values.isEmpty()) 0 else _values[_values.size / 2]
        }

    override fun add(value: Int) {
        _values.add(value)
        _dirty = true
    }

    override fun remove(value: Int) {
        _values.remove(value)
        _dirty = true
    }
}