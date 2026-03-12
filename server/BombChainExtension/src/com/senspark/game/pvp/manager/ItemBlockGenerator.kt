package com.senspark.game.pvp.manager

import com.senspark.game.pvp.entity.BlockType
import com.senspark.game.pvp.info.BlockInfo
import com.senspark.game.pvp.info.IBlockInfo
import com.senspark.game.utils.WeightedRandom
import kotlin.random.Random

class ItemBlockGenerator(
    private val _char: Char,
    private val _density: Float,
    private val _types: List<BlockType>,
    private val _dropRate: List<Float>,
    private val _random: Random,
) : IBlockGenerator {
    private val _randomizer = WeightedRandom(_dropRate)

    override fun generate(pattern: IMapPattern): List<IBlockInfo> {
        val positions = pattern.find { _, _, c -> c == _char }
        val chosenPositions = positions
            .shuffled()
            .take((positions.size * _density).toInt())
        return chosenPositions.map { position ->
            val type = _types[_randomizer.random(_random)]
            BlockInfo(
                type = type,
                x = position.first,
                y = position.second,
                health = 1,
            )
        }
    }
}