package com.senspark.game.manager.claim

import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.manager.claim.dto.ClaimV4Finalize
import com.senspark.game.manager.claim.dto.ClaimV4Prep
import com.smartfoxserver.v2.entities.data.ISFSObject

interface IClaimManager {

    fun claimReward(blockRewardType: BLOCK_REWARD_TYPE): ISFSObject
    fun confirmClaimSuccess(blockRewardType: BLOCK_REWARD_TYPE): ISFSObject

    fun prepareClaimV4(blockRewardType: BLOCK_REWARD_TYPE): ClaimV4Prep
    fun finalizeClaimAfterCheck(blockRewardType: BLOCK_REWARD_TYPE, totalClaimed: Double): ClaimV4Finalize
    fun buildClaimV4SfsObject(
        tokenTypeInBlockChain: Int,
        giftDetails: List<String>,
        nonce: Int,
        signature: String,
        amount: Double,
    ): ISFSObject

    fun syncPendingClaimsFromChain()
}
