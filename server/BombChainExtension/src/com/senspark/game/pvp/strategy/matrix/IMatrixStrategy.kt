package com.senspark.game.pvp.strategy.matrix

interface IMatrixStrategy {
    fun process(state: IMatrixState): IMatrixState
}

infix fun IMatrixStrategy.then(item: IMatrixStrategy) = ListMatrixStrategy(this, item)
infix fun IMatrixStrategy.loop(loops: Int) = LoopMatrixStrategy(this, loops)