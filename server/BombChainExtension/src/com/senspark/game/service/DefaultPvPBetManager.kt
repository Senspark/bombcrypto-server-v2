package com.senspark.game.service

import com.senspark.common.IPvPBetManager

class DefaultPvPBetManager(private val _values: List<Int>) : IPvPBetManager {
    override val indices = _values.indices
    override fun destroy() = Unit

    override fun getBetValue(index: Int): Int {
        return _values[index]
    }

    override fun toIntArray(): List<Int> {
        return _values
    }
}