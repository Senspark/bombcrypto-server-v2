package com.senspark.game.pvp.manager

import com.senspark.game.pvp.entity.BlockType
import com.senspark.game.pvp.info.BlockInfo
import com.senspark.game.pvp.info.IBlockInfo
import com.senspark.game.utils.WeightedRandom
import kotlin.random.Random

class ChestBlockGenerator(
    private val _char: Char,
    private val _density: Float,
    private val _minPosition: Pair<Int, Int>,
    private val _maxPosition: Pair<Int, Int>,
    private val _types: List<BlockType>,
    private val _dropRate: List<Float>,
    private val _random: Random,
) : IBlockGenerator {
    private val _randomizer = WeightedRandom(_dropRate)

    override fun generate(pattern: IMapPattern): List<IBlockInfo> {
        if (Random.nextFloat() >= _density) {
            return emptyList()
        }
        val positions = pattern.find { x, y, c ->
            _minPosition.first <= x && x <= _maxPosition.first &&
                _minPosition.second <= y && y <= _maxPosition.second &&
                c == _char
        }
        val chosenPosition = positions
            .shuffled()
            .take(1)
        if (chosenPosition.isEmpty()) {
            return emptyList()
        }
        val position = chosenPosition.first()
        val type = _types[_randomizer.random(_random)]
        return listOf(BlockInfo(
            type = type,
            x = position.first,
            y = position.second,
            health = 1,
        ))
    }
}