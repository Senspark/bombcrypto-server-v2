package com.senspark.game.pvp.strategy.fallingblock

import com.senspark.game.pvp.info.IFallingBlockInfo

interface IFallingBlockGenerator {
    fun generate(width: Int, height: Int, playTime: Int): List<IFallingBlockInfo>
} 