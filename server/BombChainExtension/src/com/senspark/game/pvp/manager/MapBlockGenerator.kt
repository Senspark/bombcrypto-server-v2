package com.senspark.game.pvp.manager

import com.senspark.game.pvp.info.IBlockInfo

class MapBlockGenerator(
    private val _blockHealthManager: IBlockHealthManager,
    private val _softBlockDensity: Float,
) : IBlockGenerator {
    override fun generate(pattern: IMapPattern): List<IBlockInfo> {
        val softBlockGenerators = listOf(
            SoftBlockGenerator('b', 1f, _blockHealthManager),
            SoftBlockGenerator('.', _softBlockDensity, _blockHealthManager),
        )
        val softBlocks = softBlockGenerators.map {
            it.generate(pattern)
        }.flatten()
        return softBlocks
    }
}