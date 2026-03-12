package com.senspark.game.pvp.utility

import kotlin.math.pow

interface IBitEncoder<T> {
    val value: T
    fun push(value: Boolean): IBitEncoder<T>
    fun push(value: Int, size: Int): IBitEncoder<T>
    fun push(value: Long, size: Int): IBitEncoder<T>
    fun push(value: Float, precision: Int, size: Int): IBitEncoder<T>
}

class IntBitEncoder(
    private var _position: Int = 0,
) : IBitEncoder<Int> {
    private var _value = 0

    override val value get() = _value

    override fun push(value: Boolean): IntBitEncoder {
        require(_position + 1 <= 32) { "Bit encoder out of range" }
        _value = _value or ((if (value) 1 else 0) shl _position)
        ++_position
        return this
    }

    override fun push(value: Int, size: Int): IntBitEncoder {
        require(_position + size <= 32) { "Bit encoder out of range" }
        _value = _value or (value shl _position)
        _position += size
        return this;
    }

    override fun push(value: Long, size: Int): IntBitEncoder {
        require(_position + size <= 32) { "Bit encoder out of range" }
        require(false) {
            "Cannot encode long value"
        }
        return this
    }

    override fun push(value: Float, precision: Int, size: Int): IntBitEncoder {
        val multiplier = 10f.pow(precision)
        return push((value * multiplier).toInt(), size)
    }
}

class LongBitEncoder(
    private var _position: Int = 0,
) : IBitEncoder<Long> {
    private var _value = 0L

    override val value get() = _value
    override fun push(value: Boolean): LongBitEncoder {
        require(_position + 1 <= 64) { "Bit encoder out of range" }
        _value = _value or ((if (value) 1L else 0L) shl _position)
        ++_position
        return this
    }

    override fun push(value: Int, size: Int): LongBitEncoder {
        require(_position + size <= 64) { "Bit encoder out of range" }
        _value = _value or (value.toLong() shl _position)
        _position += size
        return this;
    }

    override fun push(value: Long, size: Int): LongBitEncoder {
        require(_position + size <= 64) { "Bit encoder out of range" }
        _value = _value or (value shl _position)
        _position += size
        return this;
    }

    override fun push(value: Float, precision: Int, size: Int): LongBitEncoder {
        val multiplier = 10f.pow(precision)
        return push((value * multiplier).toInt(), size)
    }
}