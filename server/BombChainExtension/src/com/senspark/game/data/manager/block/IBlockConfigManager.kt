package com.senspark.game.data.manager.block

import com.senspark.common.service.IGlobalService
import com.senspark.game.data.model.config.BlockConfig
import com.senspark.game.declare.EnumConstants.DataType

interface IBlockConfigManager : IGlobalService {
    fun getConfig(dataType: DataType, type: Int): BlockConfig
}