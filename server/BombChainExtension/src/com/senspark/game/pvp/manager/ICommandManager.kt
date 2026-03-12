package com.senspark.game.pvp.manager

import com.senspark.common.pvp.IMoveHeroData
import com.senspark.common.pvp.IPlantBombData
import com.senspark.game.constant.Booster
import com.senspark.game.pvp.data.IMatchObserveData

interface ICommandManager {
    suspend fun moveHero(slot: Int, timestamp: Int, x: Float, y: Float): IMoveHeroData
    suspend fun plantBomb(slot: Int, timestamp: Int): IPlantBombData
    suspend fun throwBomb(slot: Int, timestamp: Int)
    suspend fun useBooster(slot: Int, timestamp: Int, item: Booster)
    fun processCommands(): List<IMatchObserveData>
}