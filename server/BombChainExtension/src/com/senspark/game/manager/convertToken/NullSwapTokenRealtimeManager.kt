package com.senspark.game.manager.convertToken

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE

class NullSwapTokenRealtimeManager : ISwapTokenRealtimeManager {
    override fun initialize() {
    }
    
    override fun reloadTokenPrice(): Map<DataType, Map<BLOCK_REWARD_TYPE, Float>> {
        return emptyMap()
    }

    override fun previewConversion(balance: Float, networkType: ISwapTokenRealtimeManager.NetworkType, tokenType: Int): Float {
        return 0f
    }

    override fun tokenConvert(
        userId: Int,
        userController: IUserController,
        balance: Float,
        networkType: ISwapTokenRealtimeManager.NetworkType,
        tokenType: Int
    ): Float {
        return 0f
    }

    override fun refillRemainingTotalSwap() {}

    override fun getTimeUpdatePrice(): Int {
        return 0
    }

    override fun getMinGemSwap(): Int {
        return 0
    }

    override fun destroy() {

    }
}