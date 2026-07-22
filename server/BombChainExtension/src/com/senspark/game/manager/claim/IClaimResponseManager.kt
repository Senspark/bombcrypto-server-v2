package com.senspark.game.manager.claim

import com.senspark.common.service.IServerService
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE

interface IClaimResponseManager : IServerService {
    fun beginCheck(
        controller: IUserController,
        walletAddress: String,
        blockRewardType: BLOCK_REWARD_TYPE,
        tokenTypeInBlockChain: Int,
    ): String
    fun listenClaimCheck(message: String): Boolean
    fun listenClaimSign(message: String): Boolean
}


