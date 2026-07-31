package com.senspark.game.manager.nativeDeposit

import com.senspark.common.service.IServerService
import com.senspark.game.manager.nativeDeposit.dto.NativeDepositResult

/**
 * Redis-stream transport to ap-deposit-native: publishes on SV_DEPNATIVE_REQUEST_STR and correlates the
 * reply on AP_DEPNATIVE_RESULT_STR by correlationId. Awaiting a `CompletableDeferred` suspends, so a
 * slow signer no longer parks a `Dispatchers.IO` thread the way a blocking HTTP call did.
 */
interface INativeDepositResponseManager : IServerService {
    /** Read the on-chain counters (deposited, withdrawn) at confirmed depth. Throws on error/timeout. */
    suspend fun requestCounters(uid: Int, walletAddress: String, network: String): NativeDepositResult

    /** Sign the EIP-191 withdraw authorization for an already-committed cumulative. Throws on error/timeout. */
    suspend fun requestWithdrawSign(
        uid: Int,
        walletAddress: String,
        network: String,
        allowedCumulativeWei: String,
    ): NativeDepositResult

    /** AP_DEPNATIVE_RESULT_STR listener — completes the awaiting request. */
    fun listenNativeResult(message: String): Boolean
}
