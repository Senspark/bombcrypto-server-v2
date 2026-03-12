package com.senspark.game.manager.claim

import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class NullClaimManagerPolygon : IClaimManager {
    override fun claimReward(blockRewardType: BLOCK_REWARD_TYPE): ISFSObject {
        return SFSObject()
    }
    
    override fun confirmClaimSuccess(blockRewardType: BLOCK_REWARD_TYPE): ISFSObject {
        return SFSObject()
    }
}