package com.senspark.game.pvp.info

import com.senspark.game.pvp.entity.BlockType
import com.senspark.game.pvp.utility.IRandomizer

interface IMapInfo {
    /** Play time in milliseconds. */
    val playTime: Int

    /** Tileset ID. */
    val tileset: Int

    /** Width of the map. */
    val width: Int

    /** Height of the map. */
    val height: Int

    /** Starting points of all users. */
    val startingPositions: List<Pair<Int, Int>>

    val blocks: List<IBlockInfo>
    val fallingBlocks: List<IFallingBlockInfo>

    /** Lower-left to upper-right area. */
    val chestBlockArea: Pair<Pair<Int, Int>, Pair<Int, Int>>
    val chestBlockDropRate: Float
    val chestBlockRandomizer: IRandomizer<BlockType>

    val itemBlockDropRate: Float
    val itemBlockRandomizer: IRandomizer<BlockType>
}