package com.senspark.game.pvp.utility

import kotlin.math.pow

interface IBitDecoder {
    fun popBoolean(): Boolean
    fun popInt(size: Int): Int
    fun popFloat(precision: Int, size: Int): Float
}

class IntBitDecoder(
    private var _value: Int,
) : IBitDecoder {
    override fun popBoolean(): Boolean {
        val result = (_value and 1) == 1
        _value = _value shr 1
        return result
    }

    override fun popInt(size: Int): Int {
        val result = _value and ((1 shl size) - 1)
        _value = _value shr size
        return result
    }

    override fun popFloat(precision: Int, size: Int): Float {
        val multiplier = 10f.pow(precision)
        val result = (_value and ((1 shl size) - 1)) / multiplier
        _value = _value shr size
        return result
    }
}

class LongBitDecoder(
    private var _value: Long,
) : IBitDecoder {
    override fun popBoolean(): Boolean {
        val result = (_value and 1) == 1L
        _value = _value shr 1
        return result
    }

    override fun popInt(size: Int): Int {
        val result = _value and ((1L shl size) - 1)
        _value = _value shr size
        return result.toInt()
    }

    override fun popFloat(precision: Int, size: Int): Float {
        val multiplier = 10f.pow(precision)
        val result = (_value and ((1L shl size) - 1)) / multiplier
        _value = _value shr size
        return result
    }
}