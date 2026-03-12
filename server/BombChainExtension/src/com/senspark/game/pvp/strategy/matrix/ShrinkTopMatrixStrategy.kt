package com.senspark.game.pvp.strategy.matrix

class ShrinkTopMatrixStrategy : IMatrixStrategy {
    override fun process(state: IMatrixState): IMatrixState {
        return MatrixState(
            left = state.left,
            right = state.right,
            top = state.top + 1,
            bottom = state.bottom,
            positions = state.positions,
        )
    }
}