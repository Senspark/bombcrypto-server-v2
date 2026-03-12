package com.senspark.game.manager.stake

import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.SFSObject

interface IUserStakeManager {
    @Throws(CustomException::class)
    fun stake(username: String, blockRewardType: BLOCK_REWARD_TYPE, amount: Float, allIN: Boolean): SFSObject

    @Throws(Exception::class)
    fun withdrawStake(username: String, isWithdraw: Boolean): SFSObject
}