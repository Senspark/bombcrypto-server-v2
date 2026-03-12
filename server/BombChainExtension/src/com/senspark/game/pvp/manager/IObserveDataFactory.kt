package com.senspark.game.pvp.manager

import com.senspark.game.pvp.data.IMatchObserveData
import com.senspark.game.pvp.delta.IMatchStateDelta

interface IObserveDataFactory {
    fun generate(timestamp: Long, stateDelta: IMatchStateDelta): IMatchObserveData
}