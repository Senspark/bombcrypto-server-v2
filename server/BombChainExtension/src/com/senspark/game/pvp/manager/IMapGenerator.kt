package com.senspark.game.pvp.manager

import com.senspark.game.pvp.info.IMapInfo

interface IMapGenerator {
    /** Generates map info. */
    fun generate(): IMapInfo
}