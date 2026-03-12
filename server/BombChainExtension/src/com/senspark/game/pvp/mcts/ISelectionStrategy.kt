package com.senspark.game.pvp.mcts

interface ISelectionStrategy {
    fun select(node: INode): INode
}