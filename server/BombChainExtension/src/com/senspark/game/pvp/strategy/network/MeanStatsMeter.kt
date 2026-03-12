package com.senspark.game.pvp.strategy.network

class MeanStatsMeter : IStatsMeter {
    private var _sum = 0
    private var _size = 0

    override val value get() = if (_size == 0) 0 else _sum / _size

    override fun add(value: Int) {
        _sum += value
        ++_size
    }

    override fun remove(value: Int) {
        _sum -= value
        --_size
    }
}