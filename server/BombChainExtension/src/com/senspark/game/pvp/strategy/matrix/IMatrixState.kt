package com.senspark.game.pvp.strategy.matrix

interface IMatrixState {
    val isValid: Boolean
    val left: Int
    val right: Int
    val top: Int
    val bottom: Int
    val positions: List<Pair<Int, Int>>
}