package com.senspark.game.pvp.mcts

interface IState {
    val lastAction: IAction
    val isTerminal: Boolean

    val lastPlayerIndex: Int
    val nextPlayerIndex: Int
    val actions: List<IAction>

    fun applyAction(action: IAction)
    fun applyRandomAction()
    fun getReward(playerIndex: Int): Float
    fun clone(): IState
}