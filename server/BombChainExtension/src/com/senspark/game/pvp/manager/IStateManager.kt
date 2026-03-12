package com.senspark.game.pvp.manager

import com.senspark.game.pvp.delta.IMatchStateDelta

interface IStateManager {
    val accumulativeChangeData: IMatchStateDelta?
    fun processState(): IMatchStateDelta?
}