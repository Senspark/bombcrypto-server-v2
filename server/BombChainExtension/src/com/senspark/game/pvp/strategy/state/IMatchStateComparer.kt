package com.senspark.game.pvp.strategy.state

import com.senspark.game.pvp.data.IMatchState
import com.senspark.game.pvp.delta.IMatchStateDelta

interface IMatchStateComparer {
    fun compare(
        state: IMatchState,
        lastState: IMatchState,
    ): IMatchStateDelta?
}