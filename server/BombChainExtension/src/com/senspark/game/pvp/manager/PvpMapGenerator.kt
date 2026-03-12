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
        _logger.log("[Pvp][DefaultPvpMapGenerator:generate]")
        val tileset = _config.tilesetList.random()

        // FIXME: client must support manual hard blocks.
        @Suppress("SpellCheckingInspection")
        val pattern2 = StringMapPattern(
            """
                p_b___xbbbx___b_p
                __x__xbbbbbx__x__
                _b__x_bxbxb_x__b_
                x__x_b_b_b_b_x__x
                ___bbxb_b_bxbb___
                __xbbbbxbxbbbbx__
                x__b_xbbbbbx_b__x
                _x_xb_bxbxb_bxbx_
                __xbbx_bbb_xbbx__
                ___b__bxbxb_bb___
                ___xbx_b_b_xbx___
                x__bbbbxbxbbbbx_x
                b___xb__b__bx___b
                _x___xbbxbbx___x_
                p_b___xbbbx___b_p
            """.trimIndent()
        )
//        val pattern = StringMapPattern(
//            """
//                0_b...........b_3
//                _x.x.x.x.x.x.x.x_
//                b...............b
//                .x.x.x.x.x.x.x.x.
//                .................
//                .x.x.x.x.x.x.x.x.
//                .................
//                .x.x.x.x.x.x.x.x.
//                .................
//                .x.x.x.x.x.x.x.x.
//                b...............b
//                _x.x.x.x.x.x.x.x_
//                2_b...........b_1
//            """.trimIndent()
//        )
        val pattern = StringMapPattern(
            """
                0_b.........b_3
                _x.x.x.x.x.x.x_
                b.............b
                .x.x.x.x.x.x.x.
                ...............
                .x.x.x.x.x.x.x.
                ...............
                .x.x.x.x.x.x.x.
                b.............b
                _x.x.x.x.x.x.x_
                2_b.........b_1
            """.trimIndent()
        )
        val positionGenerator = DefaultPositionGenerator("0123".toList())
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