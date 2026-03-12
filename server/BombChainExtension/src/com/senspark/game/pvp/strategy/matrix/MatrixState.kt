package com.senspark.game.pvp.strategy.matrix

class MatrixState(
    override val left: Int,
    override val right: Int,
    override val top: Int,
    override val bottom: Int,
    override val positions: List<Pair<Int, Int>>,
) : IMatrixState {
    override val isValid = left <= right && top <= bottom
}