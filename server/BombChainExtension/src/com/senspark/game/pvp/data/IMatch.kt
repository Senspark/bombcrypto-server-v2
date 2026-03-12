package com.senspark.game.pvp.data

import com.senspark.game.pvp.manager.IBombManager
import com.senspark.game.pvp.manager.IHeroManager
import com.senspark.game.pvp.manager.IMapManager
import com.senspark.game.pvp.manager.IUpdater

interface IMatch : IUpdater {
    val state: IMatchState
    val heroManager: IHeroManager
    val bombManager: IBombManager
    val mapManager: IMapManager
    fun applyState(state: IMatchState)
}