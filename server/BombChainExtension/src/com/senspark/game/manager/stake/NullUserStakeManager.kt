package com.senspark.game.manager.stake

import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.smartfoxserver.v2.entities.data.SFSObject

class NullUserStakeManager : IUserStakeManager {
    override fun stake(
        username: String,
        blockRewardType: BLOCK_REWARD_TYPE,
        amount: Float,
        allIN: Boolean
    ): SFSObject {
        return SFSObject()
    }

    override fun withdrawStake(username: String, isWithdraw: Boolean): SFSObject {
        return SFSObject()
    }
}