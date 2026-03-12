package com.senspark.game.pvp.manager

import com.senspark.common.utils.ILogger
import com.senspark.game.pvp.entity.*
import com.senspark.game.pvp.utility.IRandom
import com.senspark.game.pvp.utility.IRandomizer

class ItemBlockDropper(
    private val _blockDropRate: Float,
    private val _blockRandomizer: IRandomizer<BlockType>,
    private val _logger: ILogger,
    private val _random: IRandom,
) : IBlockDropper {
    override fun drop(mapManager: IMapManager, block: IBlock): IBlock? {
        if (_random.randomFloat(0f, 1f) >= _blockDropRate) {
            return null
        }
        val type = _blockRandomizer.random(_random)
        return Block(
            x = block.x,
            y = block.y,
            _initialState = BlockState(
                isAlive = true,
                reason = BlockReason.Dropped,
                type = type,
                health = 1,
                maxHealth = 1,
            ),
            _logger = _logger,
            _mapManager = mapManager,
        )
    }
}