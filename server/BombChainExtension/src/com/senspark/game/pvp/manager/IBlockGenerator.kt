package com.senspark.game.pvp.manager

import com.senspark.game.pvp.info.IBlockInfo

interface IBlockGenerator {
    fun generate(pattern: IMapPattern): List<IBlockInfo>
}