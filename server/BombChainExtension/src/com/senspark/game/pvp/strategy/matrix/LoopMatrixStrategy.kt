package com.senspark.game.pvp.strategy.matrix

/** Processes the processor repeatedly. */
class LoopMatrixStrategy(
    private val _strategy: IMatrixStrategy,
    private val _loops: Int,
) : IMatrixStrategy {
    override fun process(state: IMatrixState): IMatrixState {
        var currentState = state
        var index = 0
        while (index < _loops && currentState.isValid) {
            currentState = _strategy.process(currentState)
            ++index
        }
        return currentState
    }
}