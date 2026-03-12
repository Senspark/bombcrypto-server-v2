package com.senspark.game.pvp.strategy.state

import com.senspark.game.pvp.delta.IBombStateDelta
import com.senspark.game.pvp.manager.IBombManagerState

interface IBombStateComparer {
    fun compare(
        state: IBombManagerState,
        lastState: IBombManagerState,
    ): List<IBombStateDelta>
}