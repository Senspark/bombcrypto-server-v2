package com.senspark.game.data.manager.block

import com.senspark.common.service.IGlobalService
import com.senspark.game.declare.EnumConstants

interface IBlockDropByDayManager : IGlobalService {
    fun getBlockDropRate(dataType: EnumConstants.DataType, dayPassed: Int): List<Int>
} 