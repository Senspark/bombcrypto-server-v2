package com.senspark.game.pvp.strategy.matrix

class ProcessMatrixStrategy(
    side: MatrixSide,
    ccw: Boolean,
) : IMatrixStrategy {
    private val _strategy = when (side) {
        MatrixSide.Top -> ProcessTopMatrixStrategy(ccw)
        MatrixSide.Right -> ProcessRightMatrixStrategy(ccw)
        MatrixSide.Bottom -> ProcessBottomMatrixStrategy(ccw)
        MatrixSide.Left -> ProcessLeftMatrixStrategy(ccw)
    }

    override fun process(state: IMatrixState): IMatrixState {
        return _strategy.process(state)
    }
}