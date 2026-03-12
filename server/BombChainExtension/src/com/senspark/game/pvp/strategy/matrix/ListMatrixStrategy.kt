package com.senspark.game.pvp.strategy.matrix

/** Processes the processor list. */
class ListMatrixStrategy(
    vararg items: IMatrixStrategy,
) : IMatrixStrategy {
    private val _items = listOf(*items)

    override fun process(state: IMatrixState): IMatrixState {
        var currentState = state
        for (processor in _items) {
            if (!currentState.isValid) {
                break
            }
            currentState = processor.process(currentState)
        }
        return currentState
    }
}