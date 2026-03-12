package com.senspark.game.pvp.strategy.matrix

class ShrinkLeftMatrixStrategy : IMatrixStrategy {
    override fun process(state: IMatrixState): IMatrixState {
        return MatrixState(
            left = state.left + 1,
            right = state.right,
            top = state.top,
            bottom = state.bottom,
            positions = state.positions,
        )
    }
}