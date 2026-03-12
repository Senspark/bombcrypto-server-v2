package com.senspark.game.pvp.strategy.matrix

class ShrinkRightMatrixStrategy : IMatrixStrategy {
    override fun process(state: IMatrixState): IMatrixState {
        return MatrixState(
            left = state.left,
            right = state.right - 1,
            top = state.top,
            bottom = state.bottom,
            positions = state.positions,
        )
    }
}