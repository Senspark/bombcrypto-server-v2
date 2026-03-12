package com.senspark.game.pvp.strategy.matrix

class ShrinkMatrixStrategy(
    side: MatrixSide,
) : IMatrixStrategy {
    private val _strategy = when (side) {
        MatrixSide.Top -> ShrinkTopMatrixStrategy()
        MatrixSide.Right -> ShrinkRightMatrixStrategy()
        MatrixSide.Bottom -> ShrinkBottomMatrixStrategy()
        MatrixSide.Left -> ShrinkLeftMatrixStrategy()
    }

    override fun process(state: IMatrixState): IMatrixState {
        return _strategy.process(state)
    }
}