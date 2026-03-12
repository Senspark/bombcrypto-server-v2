package com.senspark.game.manager.blockReward

import com.senspark.game.declare.EnumConstants.TokenType
import com.senspark.game.exception.CustomException

class NullUserMiningModeManager : IUserMiningModeManager {
    override val miningMode: TokenType get() = throw CustomException("Feature not support")

    override fun changeMiningMode(tokenType: TokenType) {}
}