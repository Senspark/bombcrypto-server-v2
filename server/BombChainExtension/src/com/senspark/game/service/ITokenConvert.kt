package com.senspark.game.service

import com.senspark.common.service.IService
import com.senspark.common.service.Service
import com.senspark.game.data.model.config.SwapTokenConfig

@Service("ITokenConvert")
interface ITokenConvert : IService {
    fun setConfig(swapTokenConfig: List<SwapTokenConfig>)
    fun previewConversion(balance: Float, networkType: Int, tokenType: Int): Float
}