package com.senspark.game.pvp.manager

import com.senspark.common.utils.ILogger
import com.senspark.game.pvp.entity.BlockType
import com.senspark.game.pvp.entity.IBlock
import com.senspark.game.pvp.utility.IRandom
import com.senspark.game.pvp.utility.IRandomizer

class ChestBlockDropper(
    private val _blockDropArea: Pair<Pair<Int, Int>, Pair<Int, Int>>,
    private val _blockDropRate: Float,
    private val _blockRandomizer: IRandomizer<BlockType>,
    private val _logger: ILogger,
    random: IRandom,
) : IBlockDropper {
    private val _dropper = ItemBlockDropper(_blockDropRate, _blockRandomizer, _logger, random)

    override fun drop(mapManager: IMapManager, block: IBlock): IBlock? {
        if (!mapManager.canDropChestBlock) {
            return null
        }
        if (!(_blockDropArea.first.first <= block.x && block.x <= _blockDropArea.second.first)) {
            return null
        }
        if (!(_blockDropArea.first.second <= block.y && block.y <= _blockDropArea.second.second)) {
            return null
        }
        return _dropper.drop(mapManager, block)
    }
}