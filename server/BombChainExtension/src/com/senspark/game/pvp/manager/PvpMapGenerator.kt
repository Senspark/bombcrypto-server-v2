package com.senspark.game.pvp.manager

import com.senspark.common.utils.ILogger
import com.senspark.game.pvp.config.IMapConfig
import com.senspark.game.pvp.entity.BlockType
import com.senspark.game.pvp.info.MapInfo
import com.senspark.game.pvp.strategy.fallingblock.toGenerator
import com.senspark.game.pvp.utility.WeightedRandomizer

class PvpMapGenerator(
    private val _config: IMapConfig,
    private val _chestBlockRadius: Pair<Int, Int>,
    private val _chestBlockDensity: Float,
    private val _chestBlockTypes: List<BlockType>,
    private val _chestBlockDropRates: List<Float>,
    private val _itemBlockDensity: Float,
    private val _itemBlockTypes: List<BlockType>,
    private val _itemBlockDropRates: List<Float>,
    private val _blockGenerator: IBlockGenerator,
    private val _logger: ILogger,
) : IMapGenerator {
    override fun generate(): MapInfo {
        _logger.log("[Pvp][DefaultPvpMapGenerator:generate] Pattern: ${_config.mapPatternId}")
        val tileset = _config.tilesetList.random()

        val pattern = StringMapPattern(MapPatternRegistry.getPattern(_config.mapPatternId))
        
        val spawnIds = (0 until _config.maxPlayers).joinToString("")
        val positionGenerator = DefaultPositionGenerator(spawnIds.toList())
        val blocks = _blockGenerator.generate(pattern)
        val fallingBlockPattern = _config.fallingBlockPatternList.random()
        val fallingBlockGenerator = fallingBlockPattern.toGenerator()
        val center = Pair(pattern.width / 2, pattern.height / 2)
        val minPosition = Pair(center.first - _chestBlockRadius.first, center.second - _chestBlockRadius.second)
        val maxPosition = Pair(center.first + _chestBlockRadius.first, center.second + _chestBlockRadius.second)
        return MapInfo(
            playTime = _config.playTime,
            tileset = tileset,
            width = pattern.width,
            height = pattern.height,
            startingPositions = positionGenerator.generate(pattern),
            blocks = blocks,
            fallingBlocks = fallingBlockGenerator.generate(pattern.width, pattern.height, _config.playTime),
            chestBlockArea = minPosition to maxPosition,
            chestBlockDropRate = _chestBlockDensity,
            chestBlockRandomizer = WeightedRandomizer(_chestBlockTypes, _chestBlockDropRates),
            itemBlockDropRate = _itemBlockDensity,
            itemBlockRandomizer = WeightedRandomizer(_itemBlockTypes, _itemBlockDropRates),
        )
    }
}