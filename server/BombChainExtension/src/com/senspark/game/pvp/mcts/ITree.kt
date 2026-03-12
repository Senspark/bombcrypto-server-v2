package com.senspark.game.pvp.mcts

interface ITree {
    fun computeAction(root: INode): IAction
}