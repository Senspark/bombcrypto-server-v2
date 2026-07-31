package com.senspark.game.manager.nativeDeposit

import com.senspark.common.cache.IMessengerService
import com.senspark.common.utils.ILogger
import com.senspark.game.constant.StreamKeys
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.nativeDeposit.dto.NativeDepositRequestKind
import com.senspark.game.manager.nativeDeposit.dto.NativeDepositResult
import com.senspark.game.utils.JsonExtensionBuilder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class NativeDepositResponseManager(
    private val _messenger: IMessengerService,
    private val _logger: ILogger,
    // Bounds a stuck signer or Redis, nothing else. A counters read is two confirmed-depth eth_calls
    // through the blockchain center, so it is slower than the bridge's single view call.
    private val _requestTimeoutMs: Long = 15 * 1000L,
    private val _stateTtlMs: Long = 60 * 1000L,
) : INativeDepositResponseManager {

    private class PendingState(
        val correlationId: String,
        val kind: String,
        val uid: Int,
        val createdAtMs: Long,
        val resultDeferred: CompletableDeferred<NativeDepositResult>,
    )

    private val _pending = ConcurrentHashMap<String, PendingState>()

    private val tagResult = "[${StreamKeys.AP_DEPNATIVE_RESULT_STR}]"

    override suspend fun requestCounters(uid: Int, walletAddress: String, network: String): NativeDepositResult {
        return exchange(NativeDepositRequestKind.COUNTERS, uid) {
            put("wallet", walletAddress)
            put("network", network)
        }
    }

    override suspend fun requestWithdrawSign(
        uid: Int,
        walletAddress: String,
        network: String,
        allowedCumulativeWei: String,
    ): NativeDepositResult {
        return exchange(NativeDepositRequestKind.WITHDRAW_SIGN, uid) {
            put("wallet", walletAddress)
            put("network", network)
            put("allowedCumulative", allowedCumulativeWei)
        }
    }

    override fun listenNativeResult(message: String): Boolean {
        try {
            val response = JsonExtensionBuilder.json.decodeFromString<NativeDepositResult>(message)
            val state = getActive(response.correlationId) ?: run {
                // Either the other zone container owns this correlation, or the waiter already timed out.
                // Confirmed either way so the message leaves the stream.
                return true
            }
            state.resultDeferred.complete(response)
            return true
        } catch (e: Exception) {
            _logger.error("$tagResult Error processing message: ${e.message}", e)
            return false
        }
    }

    override fun initialize() {}

    // --- helpers ---

    private suspend fun exchange(
        kind: String,
        uid: Int,
        fields: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit,
    ): NativeDepositResult {
        sweepExpired()
        val state = PendingState(
            correlationId = UUID.randomUUID().toString(),
            kind = kind,
            uid = uid,
            createdAtMs = System.currentTimeMillis(),
            resultDeferred = CompletableDeferred(),
        )
        _pending[state.correlationId] = state
        try {
            val payload = buildJsonObject {
                put("correlationId", state.correlationId)
                put("kind", kind)
                put("uid", uid)
                fields()
            }
            _messenger.send(StreamKeys.SV_DEPNATIVE_REQUEST_STR, payload.toString())

            val result = try {
                withTimeout(_requestTimeoutMs) { state.resultDeferred.await() }
            } catch (e: TimeoutCancellationException) {
                _logger.error("$tagResult timeout kind=$kind uid=$uid cid=${state.correlationId} after ${_requestTimeoutMs}ms")
                throw CustomException(SIGNER_UNAVAILABLE)
            }
            if (result.code != CODE_OK) {
                // ap-deposit-native already sanitized errorMessage; the raw cause stays in its own log.
                _logger.error("$tagResult error kind=$kind uid=$uid cid=${state.correlationId} code=${result.code} msg=${result.errorMessage}")
                throw CustomException(result.errorMessage ?: SIGNER_UNAVAILABLE)
            }
            return result
        } finally {
            _pending.remove(state.correlationId)
        }
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
            val entry = iterator.next()
            if (isExpired(entry.value)) {
                _logger.warn("$tagResult sweeping expired cid=${entry.key} kind=${entry.value.kind} uid=${entry.value.uid}")
                iterator.remove()
            }
        }
    }

    private fun isExpired(state: PendingState): Boolean {
        return System.currentTimeMillis() - state.createdAtMs > _stateTtlMs
    }

    companion object {
        private const val CODE_OK = 0
        private const val SIGNER_UNAVAILABLE = "Unable to reach the signer right now. Please try again later."
    }
}
