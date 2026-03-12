package com.senspark.game.pvp.manager

import com.senspark.game.pvp.entity.BlockType
import com.senspark.game.pvp.info.BlockInfo
import com.senspark.game.pvp.info.IBlockInfo

class SoftBlockGenerator(
    private val _char: Char,
    private val _density: Float,
    private val _blockHealthManager: IBlockHealthManager,
) : IBlockGenerator {
    override fun generate(pattern: IMapPattern): List<IBlockInfo> {
        val positions = pattern.find { _, _, c -> c == _char }
        val chosenPositions = positions
            .shuffled()
            .take((positions.size * _density).toInt())
        return chosenPositions.map { position ->
            val type = BlockType.Soft
            BlockInfo(
                type = type,
                x = position.first,
                y = position.second,
                health = _blockHealthManager.getHealth(type),
            )
        }
    }
}