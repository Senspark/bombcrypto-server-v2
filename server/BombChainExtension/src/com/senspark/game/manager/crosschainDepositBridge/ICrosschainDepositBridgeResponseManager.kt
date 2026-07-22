package com.senspark.game.manager.crosschainDepositBridge

import com.senspark.common.service.IServerService
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.manager.crosschainDepositBridge.dto.CrosschainDepositBridgeChain
import com.senspark.game.manager.crosschainDepositBridge.dto.CrosschainDepositBridgeWithdrawOutcome

/**
 * Server-scoped gateway between the client and the ap-deposit-bridge SIGNER (Phase 9). Forwards a
 * withdraw-sign request over Redis (SV_DEPBRIDGE_REQUEST_STR) and correlates the reply
 * (AP_DEPBRIDGE_RESULT_STR). Signing is synchronous — no push leg, no pending, no balance mutation.
 */
interface ICrosschainDepositBridgeResponseManager : IServerService {
    /**
     * Forward a withdraw-sign request and **suspend until the signer replies**. Returns an
     * [CrosschainDepositBridgeWithdrawOutcome]: Ok(payload) — the signature + relay parameters — to send on
     * the request, or Rejected(code, safeMessage) for the handler to `sendError` (business errors are never
     * thrown as CustomException, so no backend/RPC text can leak). A timeout still throws.
     */
    suspend fun beginWithdraw(
        controller: IUserController,
        walletAddress: String,
        rewardType: BLOCK_REWARD_TYPE,
        withdrawChain: CrosschainDepositBridgeChain,
    ): CrosschainDepositBridgeWithdrawOutcome

    /** AP_DEPBRIDGE_RESULT_STR listener — complete the awaiting withdraw-sign request. */
    fun listenBridgeResult(message: String): Boolean

    /**
     * Fire-and-forget bridge activity notify → SV_DEPBRIDGE_NOTIFY_STR. Just kicks the indexer's sweep into
     * fast mode; no reply, never a safety gate. The wallet is taken server-side (session), never the client.
     */
    fun publishNotify(
        kind: String,
        wallet: String,
        chain: CrosschainDepositBridgeChain,
        rewardType: BLOCK_REWARD_TYPE,
        txHash: String?,
    )
}
