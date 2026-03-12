package com.senspark.game.pvp.strategy.matrix

enum class MatrixSide {
    Top,
    Right,
    Bottom,
    Left,
}

fun MatrixSide.nextSides(ccw: Boolean, skip: Int, count: Int): List<MatrixSide> {
    val values = MatrixSide.values()
    return (0 until count).map {
        val index = skip + it
        values[((ordinal + if (ccw) -index else +index) % values.size + values.size) % values.size]
    }
}