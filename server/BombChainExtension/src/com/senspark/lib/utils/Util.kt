package com.senspark.lib.utils

import java.util.*

object Util {
    private val rand = Random()
    fun randInt(min: Int, max: Int): Int {
        return rand.nextInt(max - min + 1) + min
    }

    fun randFloat(min: Float, max: Float): Float {
        return rand.nextFloat() * (max - min) + min
    }

    fun randIndex(size: Int): Int {
        return rand.nextInt(size)
    }
}