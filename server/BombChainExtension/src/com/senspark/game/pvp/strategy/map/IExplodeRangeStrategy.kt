package com.senspark.game.pvp.strategy.map

import com.senspark.game.pvp.entity.Direction
import com.senspark.game.pvp.manager.IMapManager

interface IExplodeRangeStrategy {
    fun getExplodeRange(
        manager: IMapManager,
        x: Int,
        y: Int,
        range: Int,
        piercing: Boolean,
        direction: Direction,
    ): Int
}