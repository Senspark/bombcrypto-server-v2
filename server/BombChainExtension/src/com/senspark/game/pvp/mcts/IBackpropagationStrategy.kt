package com.senspark.game.pvp.mcts

interface IBackpropagationStrategy {
    fun update(node: INode, finalState: IState)
}