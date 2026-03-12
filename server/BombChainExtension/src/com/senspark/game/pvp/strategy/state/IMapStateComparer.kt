package com.senspark.game.pvp.strategy.state

import com.senspark.game.pvp.delta.IBlockStateDelta
import com.senspark.game.pvp.manager.IMapManagerState

interface IMapStateComparer {
    fun compare(
        state: IMapManagerState,
        lastState: IMapManagerState,
    ): List<IBlockStateDelta>
}