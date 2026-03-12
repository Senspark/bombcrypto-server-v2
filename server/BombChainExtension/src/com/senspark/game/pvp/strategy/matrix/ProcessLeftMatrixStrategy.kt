package com.senspark.game.pvp.strategy.matrix

/** Processes the left-most column. */
class ProcessLeftMatrixStrategy(private val _ccw: Boolean) : IMatrixStrategy {
    override fun process(state: IMatrixState): IMatrixState {
        val range = if (_ccw) state.top..state.bottom else state.bottom downTo state.top
        return MatrixState(
            left = state.left,
            right = state.right,
            top = state.top,
            bottom = state.bottom,
            positions = state.positions + range.map { state.left to it }
        )
    }
}