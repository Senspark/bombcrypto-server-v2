package com.senspark.game.pvp.data

import com.senspark.game.pvp.manager.IBombManagerState
import com.senspark.game.pvp.manager.IHeroManagerState
import com.senspark.game.pvp.manager.IMapManagerState

interface IMatchState {
    val heroState: IHeroManagerState
    val bombState: IBombManagerState
    val mapState: IMapManagerState
    fun apply(state: IMatchState): IMatchState
}