package com.senspark.game.manager.convertToken

import com.senspark.common.service.IServerService
import com.senspark.common.service.IService
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants

interface ISwapTokenRealtimeManager : IService, IServerService {
    fun reloadTokenPrice(): Map<EnumConstants.DataType, Map<EnumConstants.BLOCK_REWARD_TYPE, Float>>
    fun previewConversion(balance: Float, networkType: NetworkType, tokenType: Int): Float
    fun tokenConvert(
        userId: Int,
        userController: IUserController,
        balance: Float,
        networkType: NetworkType,
        tokenType: Int
    ): Float

    fun refillRemainingTotalSwap()
    fun getTimeUpdatePrice(): Int
    fun getMinGemSwap(): Int

    enum class NetworkType(val value: Int) {
        UNKNOWN(-1),
        BNB(0),
        POLYGON(1),
        TON(2),
        SOL(3),
        ;
        
        companion object {
            fun from(value: Int) = entries.firstOrNull { it.value == value } ?: UNKNOWN
        }
    }
}

