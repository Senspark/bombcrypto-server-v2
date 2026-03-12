package com.senspark.game.pvp.strategy.map

import com.senspark.game.pvp.entity.IBomb
import com.senspark.game.pvp.manager.IBombManager
import com.senspark.game.pvp.manager.IMapManager

interface IExpandResult {
    val damagedPositions: Map<Pair<Int, Int>, Int>
    val explodedBombs: List<IBomb>
}

interface IExpandStrategy {
    fun expand(
        bombManager: IBombManager,
        mapManager: IMapManager,
        bomb: IBomb,
    ): IExpandResult
}