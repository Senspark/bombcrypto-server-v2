package com.senspark.game.utils

class DetailsExtractor(details: String) {
    val value = details.toBigInteger()

    fun extract(position: Int, size: Int): Int {
        require(size <= 30)
        val mask = ((1 shl size) - 1).toBigInteger()
        return ((value shr position) and mask).toInt()
    }

    fun extractArray(position: Int, size: Int, elementSize: Int): List<Int> {
        var currentPosition = position
        val arraySize = extract(currentPosition, size)
        currentPosition += size
        val array = mutableListOf<Int>()
        for (i in 0 until arraySize) {
            array.add(extract(currentPosition, elementSize))
            currentPosition += elementSize
        }
        return array
    }
}