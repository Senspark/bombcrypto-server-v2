package com.senspark.game.pvp.strategy.state

import com.senspark.game.pvp.delta.IHeroStateDelta
import com.senspark.game.pvp.manager.IHeroManagerState

interface IHeroStateComparer {
    fun compare(
        state: IHeroManagerState,
        lastState: IHeroManagerState,
    ): List<IHeroStateDelta>
}