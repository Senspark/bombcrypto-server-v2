package com.senspark.game.manager.nativeDeposit

import com.senspark.common.service.IServerService
import com.senspark.game.db.model.NativeLedger
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.smartfoxserver.v2.entities.data.ISFSObject

/**
 * Server-scoped gateway for native (BNB / POL) deposit + withdraw. The game server owns the wei ledger
 * and the row lock; ap-deposit-native is a pure counter-reader + EIP-191 signer. Fail-closed: any
 * read/DB/sign error means no signature.
 */
interface INativeDepositManager : IServerService {
    /** Read on-chain counters, run the row-locked withdraw request, sign; returns the payload to send. */
    suspend fun requestWithdraw(uid: Int, walletAddress: String, rewardType: BLOCK_REWARD_TYPE): ISFSObject

    /** Read on-chain counters and run the idempotent sync (settle + re-project). Returns the wei ledger. */
    suspend fun sync(uid: Int, walletAddress: String, rewardType: BLOCK_REWARD_TYPE): NativeLedger

    /** Settle every (uid, network) with an outstanding pending withdraw (joined to its wallet). */
    suspend fun reconcilePending()
}
