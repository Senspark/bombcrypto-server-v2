package com.senspark.game.user

import com.senspark.game.pvp.IUserPoint
import java.lang.Integer.max

class UserPoint(
    private var _value: Int
) : IUserPoint {
    override val value get() = _value

    fun add(value: Int): Int {
        val oldVal = _value
        _value = max(0, oldVal + value)
        val result = _value - oldVal
        return result
    }

    operator fun plusAssign(value: Int) {
        _value += value
    }
}