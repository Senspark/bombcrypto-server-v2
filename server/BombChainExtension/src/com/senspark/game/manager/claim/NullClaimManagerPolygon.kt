package com.senspark.game.manager.claim

import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.claim.dto.ClaimV4Finalize
import com.senspark.game.manager.claim.dto.ClaimV4Prep
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class NullClaimManagerPolygon : IClaimManager {
    override fun claimReward(blockRewardType: BLOCK_REWARD_TYPE): ISFSObject {
        return SFSObject()
    }

    override fun confirmClaimSuccess(blockRewardType: BLOCK_REWARD_TYPE): ISFSObject {
        return SFSObject()
    }

    override fun prepareClaimV4(blockRewardType: BLOCK_REWARD_TYPE): ClaimV4Prep {
        throw CustomException("Claim not supported on this server")
    }

    override fun finalizeClaimAfterCheck(
        blockRewardType: BLOCK_REWARD_TYPE,
        totalClaimed: Double,
    ): ClaimV4Finalize {
        throw CustomException("Claim not supported on this server")
    }

    override fun buildClaimV4SfsObject(
        tokenTypeInBlockChain: Int,
        giftDetails: List<String>,
        nonce: Int,
        signature: String,
        amount: Double,
    ): ISFSObject {
        return SFSObject()
    }

    override fun syncPendingClaimsFromChain() {}
}
