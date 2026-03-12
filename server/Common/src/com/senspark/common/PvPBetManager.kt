package com.senspark.common

import com.senspark.common.service.IService
import com.senspark.common.service.Service

@Service("IPvPBetManager")
interface IPvPBetManager: IService {
    val indices: IntRange
    fun getBetValue(index: Int): Int
    fun toIntArray(): List<Int>
}
