package com.senspark.game.data.manager.block

import com.senspark.game.data.model.config.BlockConfig
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.declare.EnumConstants.DataType

class BlockConfigManager(
    private val _shopDataAccess: IShopDataAccess,
) : IBlockConfigManager {
    private val _data: MutableMap<DataType, Map<Int, BlockConfig>> = mutableMapOf()

    override fun initialize() {
        _data.putAll(_shopDataAccess.loadBlock())
    }

    override fun getConfig(dataType: DataType, type: Int): BlockConfig {
        return _data[dataType]?.get(type) ?: throw IllegalArgumentException("Invalid type: $type")
    }
}