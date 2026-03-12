package com.senspark.game.manager.blockReward

import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.EnumConstants.TokenType

interface IUserMiningModeManager {
    val miningMode: TokenType

    fun changeMiningMode(tokenType: TokenType)
}