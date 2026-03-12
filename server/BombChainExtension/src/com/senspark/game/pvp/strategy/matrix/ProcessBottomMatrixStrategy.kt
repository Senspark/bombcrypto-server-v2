package com.senspark.game.pvp.strategy.matrix

/** Processes the bottom-most row. */
class ProcessBottomMatrixStrategy(private val _ccw: Boolean) : IMatrixStrategy {
    override fun process(state: IMatrixState): IMatrixState {
        val range = if (_ccw) state.left..state.right else state.right downTo state.left
        return MatrixState(
            left = state.left,
            right = state.right,
            top = state.top,
            bottom = state.bottom,
            positions = state.positions + range.map { it to state.bottom }
        )
    }
}