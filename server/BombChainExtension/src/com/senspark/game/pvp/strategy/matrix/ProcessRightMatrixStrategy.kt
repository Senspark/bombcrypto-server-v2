package com.senspark.game.pvp.strategy.matrix

/** Processes the right-most column. */
class ProcessRightMatrixStrategy(private val _ccw: Boolean) : IMatrixStrategy {
    override fun process(state: IMatrixState): IMatrixState {
        val range = if (_ccw) state.bottom downTo state.top else state.top..state.bottom
        return MatrixState(
            left = state.left,
            right = state.right,
            top = state.top,
            bottom = state.bottom,
            positions = state.positions + range.map { state.right to it }
        )
    }
}