package com.senspark.game.pvp.strategy.matrix

class ShrinkBottomMatrixStrategy : IMatrixStrategy {
    override fun process(state: IMatrixState): IMatrixState {
        return MatrixState(
            left = state.left,
            right = state.right,
            top = state.top,
            bottom = state.bottom - 1,
            positions = state.positions,
        )
    }
}