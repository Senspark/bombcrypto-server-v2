package com.senspark.game.manager.claim

import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.smartfoxserver.v2.entities.data.ISFSObject

interface IClaimManager {

    fun claimReward(blockRewardType: BLOCK_REWARD_TYPE): ISFSObject
    fun confirmClaimSuccess(blockRewardType: BLOCK_REWARD_TYPE): ISFSObject

}