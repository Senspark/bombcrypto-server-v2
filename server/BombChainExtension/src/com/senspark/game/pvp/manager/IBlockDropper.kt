package com.senspark.game.pvp.manager

import com.senspark.game.pvp.entity.IBlock

interface IBlockDropper {
    fun drop(mapManager: IMapManager, block: IBlock): IBlock?
}