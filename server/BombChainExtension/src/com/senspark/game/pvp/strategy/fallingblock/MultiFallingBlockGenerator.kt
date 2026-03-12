package com.senspark.game.pvp.strategy.fallingblock

import com.senspark.game.pvp.info.IFallingBlockInfo

class MultiFallingBlockGenerator(
    private val _generators: List<IFallingBlockGenerator>
) : IFallingBlockGenerator {
    override fun generate(width: Int, height: Int, playTime: Int): List<IFallingBlockInfo> {
        return _generators
            .flatMap { it.generate(width, height, playTime) }
            .sortedBy { it.timestamp }
    }
}