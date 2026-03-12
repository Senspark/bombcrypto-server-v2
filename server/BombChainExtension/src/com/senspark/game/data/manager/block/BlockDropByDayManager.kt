package com.senspark.game.data.manager.block

import com.senspark.game.data.model.config.BlockDropRate
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.declare.EnumConstants.DataType

class BlockDropByDayManager(
    private val _shopDataAccess: IShopDataAccess,
) : IBlockDropByDayManager {

    private val _blockDropRateMap: MutableMap<DataType, List<BlockDropRate>> = mutableMapOf()

    override fun initialize() {
        _blockDropRateMap.putAll(_shopDataAccess.loadBlockDropByDay())
    }

    override fun getBlockDropRate(dataType: DataType, dayPassed: Int): List<Int> {
        val lstSortByDay = _blockDropRateMap[dataType]!!
        var result = lstSortByDay[lstSortByDay.size - 1].dropRate
        for (blockDropRate in lstSortByDay) {
            if (dayPassed < blockDropRate.day) {
                result = blockDropRate.dropRate
                break
            }
        }
        return result
    }
}