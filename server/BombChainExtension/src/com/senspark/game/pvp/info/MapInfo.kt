package com.senspark.game.pvp.info

import com.senspark.game.pvp.entity.BlockType
import com.senspark.game.pvp.utility.IRandomizer
import com.senspark.game.pvp.utility.WeightedRandomizer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class MapInfo(
    @SerialName("play_time") override val playTime: Int,
    @SerialName("tileset") override val tileset: Int,
    @SerialName("width") override val width: Int,
    @SerialName("height") override val height: Int,
    @SerialName("starting_positions") override val startingPositions: List<Pair<Int, Int>>,
    @SerialName("blocks") override val blocks: List<IBlockInfo>,
    @Transient override val fallingBlocks: List<IFallingBlockInfo> = emptyList(),
    @Transient override val chestBlockArea: Pair<Pair<Int, Int>, Pair<Int, Int>> = (0 to 0) to (0 to 0),
    @Transient override val chestBlockDropRate: Float = 0f,
    @Transient override val chestBlockRandomizer: IRandomizer<BlockType> = WeightedRandomizer(emptyList(), emptyList()),
    @Transient override val itemBlockDropRate: Float = 0f,
    @Transient override val itemBlockRandomizer: IRandomizer<BlockType> = WeightedRandomizer(emptyList(), emptyList()),
) : IMapInfo 