package com.senspark.game.manager.crosschainDepositBridge

import com.senspark.common.cache.IMessengerService
import com.senspark.common.utils.ILogger
import com.senspark.game.constant.StreamKeys
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.ErrorCode
import com.senspark.game.extension.coroutines.ICoroutineScope
import com.senspark.game.manager.IUsersManager
import com.senspark.game.manager.crosschainDepositBridge.dto.CrosschainDepositBridgeChain
import com.senspark.game.manager.crosschainDepositBridge.dto.CrosschainDepositBridgeNotifyKind
import com.senspark.game.manager.crosschainDepositBridge.dto.CrosschainDepositBridgeResult
import com.senspark.game.manager.crosschainDepositBridge.dto.CrosschainDepositBridgeWithdrawOutcome
import com.senspark.game.manager.crosschainDepositBridge.dto.crosschainDepositBridgeChainFromClientOrNull
import com.senspark.game.utils.JsonExtensionBuilder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CrosschainDepositBridgeResponseManager(
    private val _messenger: IMessengerService,
    @Suppress("unused") private val _usersManager: IUsersManager,
    @Suppress("unused") private val _coroutine: ICoroutineScope,
    private val _logger: ILogger,
    private val _stateTtlMs: Long = 10 * 60 * 1000L,
    // Signing is synchronous (view-call + sign), so this only bounds a stuck signer/Redis. On timeout the
    // awaiting handler throws → error response (never leaves the client hanging).
    private val _requestTimeoutMs: Long = 10 * 1000L,
) : ICrosschainDepositBridgeResponseManager {

    private class PendingState(
        val correlationId: String,
        val uid: Int,
        val createdAtMs: Long,
        val resultDeferred: CompletableDeferred<CrosschainDepositBridgeResult>,
    )

    private val _pending = ConcurrentHashMap<String, PendingState>()

    private val tagResult = "[${StreamKeys.AP_DEPBRIDGE_RESULT_STR}]"

    override suspend fun beginWithdraw(
        controller: IUserController,
        walletAddress: String,
        rewardType: BLOCK_REWARD_TYPE,
        withdrawChain: CrosschainDepositBridgeChain,
    ): CrosschainDepositBridgeWithdrawOutcome {
        sweepExpired()
        val state = newState(controller)
        try {
            return withTimeout(_requestTimeoutMs) {
                forwardRequest(state, walletAddress, rewardType, withdrawChain)
                val result = state.resultDeferred.await()
                if (result.code != CODE_OK || result.signature.isNullOrEmpty()) {
                    // Signer already sanitized errorMessage to a client-safe string; never surface raw text.
                    CrosschainDepositBridgeWithdrawOutcome.Rejected(ErrorCode.SERVER_ERROR, result.errorMessage ?: DEFAULT_WITHDRAW_ERROR)
                } else {
                    val resultChain = crosschainDepositBridgeChainFromClientOrNull(result.chain) ?: withdrawChain
                    val payload = controller.masterUserManager.crosschainDepositBridgeManager.buildSignatureResult(
                        result.signature,
                        result.otherDeposited ?: "0",
                        result.deadline ?: 0L,
                        result.bridgeAddress ?: "",
                        result.tokenAddress ?: "",
                        resultChain,
                    )
                    CrosschainDepositBridgeWithdrawOutcome.Ok(payload)
                }
            }
        } finally {
            _pending.remove(state.correlationId)
        }
    }

    override fun listenBridgeResult(message: String): Boolean {
        try {
            _logger.log("$tagResult Received: $message")
            val response = JsonExtensionBuilder.json.decodeFromString<CrosschainDepositBridgeResult>(message)
            val state = getActive(response.correlationId) ?: run {
                _logger.log("$tagResult No pending state for cid=${response.correlationId}, dropping")
                return true
            }
            state.resultDeferred.complete(response)
            return true
        } catch (e: Exception) {
            _logger.error("$tagResult Error processing message: ${e.message}", e)
            return false
        }
    }

    override fun publishNotify(
        kind: String,
        wallet: String,
        chain: CrosschainDepositBridgeChain,
        rewardType: BLOCK_REWARD_TYPE,
        txHash: String?,
    ) {
        try {
            val payload = buildJsonObject {
                put("kind", kind)
                put("wallet", wallet)
                put("chain", chain.dbName)
                put("rewardType", rewardType.name)
                if (!txHash.isNullOrEmpty()) put("txHash", txHash)
                put("ts", System.currentTimeMillis())
            }
            _messenger.send(StreamKeys.SV_DEPBRIDGE_NOTIFY_STR, payload.toString())
        } catch (e: Exception) {
            // Report-only: a failed notify just means the idle sweep catches the activity a little later.
            _logger.error("[${StreamKeys.SV_DEPBRIDGE_NOTIFY_STR}] publish failed: ${e.message}", e)
        }
    }

    // --- helpers ---

    private fun forwardRequest(
        state: PendingState,
        walletAddress: String,
        rewardType: BLOCK_REWARD_TYPE,
        withdrawChain: CrosschainDepositBridgeChain,
    ) {
        val payload = buildJsonObject {
            put("correlationId", state.correlationId)
            put("kind", KIND_WITHDRAW)
            put("uid", state.uid)
            put("wallet", walletAddress)
            put("rewardType", rewardType.name)
            put("withdrawChain", withdrawChain.dbName)
        }
        _messenger.send(StreamKeys.SV_DEPBRIDGE_REQUEST_STR, payload.toString())
        // E3 — warm the withdraw-chain sweep the moment a withdraw is requested (the tx lands seconds later).
        publishNotify(CrosschainDepositBridgeNotifyKind.WITHDRAW_REQUEST, walletAddress, withdrawChain, rewardType, null)
    }

    private fun newState(controller: IUserController): PendingState {
        val state = PendingState(
            correlationId = UUID.randomUUID().toString(),
            uid = controller.userId,
            createdAtMs = System.currentTimeMillis(),
            resultDeferred = CompletableDeferred(),
        )
        _pending[state.correlationId] = state
        return state
    }

    private fun getActive(correlationId: String): PendingState? {
        val state = _pending[correlationId] ?: return null
        if (isExpired(state)) {
            _pending.remove(correlationId, state)
            return null
        }
        return state
    }

    private fun sweepExpired() {
        val iterator = _pending.entries.iterator()
        while (iterator.hasNext()) {
            if (isExpired(iterator.next().value)) iterator.remove()
        }
    }

    private fun isExpired(state: PendingState): Boolean {
        return System.currentTimeMillis() - state.createdAtMs > _stateTtlMs
    }

    override fun initialize() {}

    companion object {
        private const val KIND_WITHDRAW = "withdraw"
        private const val CODE_OK = 0
        // Client-safe fallback — the raw cause is always logged in ap-deposit-bridge, never sent here.
        private const val DEFAULT_WITHDRAW_ERROR = "Unable to process your withdraw right now. Please try again later."
    }
}
