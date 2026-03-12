package com.senspark.game.service

import com.senspark.game.data.model.config.SwapTokenConfig

class MockTokenConvert : ITokenConvert {
    override fun destroy() = Unit

    override fun setConfig(swapTokenConfig: List<SwapTokenConfig>) {
        TODO("Not yet implemented")
    }

    override fun previewConversion(balance: Float, networkType: Int, tokenType: Int): Float {
        return balance * tokenType
    }
}