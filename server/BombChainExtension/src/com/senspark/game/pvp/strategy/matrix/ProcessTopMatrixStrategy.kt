package com.senspark.game.pvp.strategy.matrix

/** Processes the top-most row. */
class ProcessTopMatrixStrategy(private val _ccw: Boolean) : IMatrixStrategy {
    override fun process(state: IMatrixState): IMatrixState {
        val range = if (_ccw) state.right downTo state.left else state.left..state.right
        return MatrixState(
            left = state.left,
            right = state.right,
            top = state.top,
            bottom = state.bottom,
            positions = state.positions + range.map { it to state.top }
        )
    }
}