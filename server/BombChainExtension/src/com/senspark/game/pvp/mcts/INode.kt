package com.senspark.game.pvp.mcts

interface INode {
    val isRoot: Boolean
    val isExpanded: Boolean
    val isTerminal: Boolean
    val parent: INode?
    val playerIndex: Int
    val action: IAction
    val state: IState

    val visitCount: Int
    val reward: Float
    val winRate: Float
    fun expand(): INode
    fun increaseVisitCount()
    fun addReward(amount: Float)

    fun selectBestChild(evaluator: (node: INode) -> Float): INode
}