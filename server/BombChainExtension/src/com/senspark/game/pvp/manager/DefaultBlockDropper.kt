package com.senspark.game.pvp.manager

import com.senspark.common.utils.ILogger
import com.senspark.game.pvp.entity.IBlock
import com.senspark.game.pvp.info.IMapInfo
import com.senspark.game.pvp.utility.IRandom

class DefaultBlockDropper private constructor(
    private val _droppers: List<IBlockDropper>,
) : IBlockDropper {
    companion object {
        fun create(
            info: IMapInfo,
            logger: ILogger,
            random: IRandom,
        ): IBlockDropper {
            return DefaultBlockDropper(
                listOf(
                    ChestBlockDropper(
                        _blockDropArea = info.chestBlockArea,
                        _blockDropRate = info.chestBlockDropRate,
                        _blockRandomizer = info.chestBlockRandomizer,
                        _logger = logger,
                        random = random,
                    ),
                    ItemBlockDropper(
                        _blockDropRate = info.itemBlockDropRate,
                        _blockRandomizer = info.itemBlockRandomizer,
                        _logger = logger,
                        _random = random,
                    ),
                )
            )
        }
    }

    override fun drop(mapManager: IMapManager, block: IBlock): IBlock? {
        return _droppers.firstNotNullOfOrNull {
            it.drop(mapManager, block)
        }
    }
}