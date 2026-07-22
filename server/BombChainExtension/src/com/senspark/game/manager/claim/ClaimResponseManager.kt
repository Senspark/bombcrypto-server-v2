package com.senspark.game.manager.claim

import com.senspark.common.utils.ILogger
import com.senspark.game.api.IRestApi
import com.senspark.game.constant.StreamKeys
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.extension.coroutines.ICoroutineScope
import com.senspark.game.manager.IEnvManager
import com.senspark.game.manager.IUsersManager
import com.senspark.game.manager.claim.dto.ClaimCheckResponse
import com.senspark.game.manager.claim.dto.ClaimSignResponse
import com.senspark.game.pvp.HandlerCommand
import com.senspark.game.utils.JsonExtensionBuilder
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ClaimResponseManager(
    private val _envManager: IEnvManager,
    private val _restApi: IRestApi,
    private val _usersManager: IUsersManager,
    private val _coroutine: ICoroutineScope,
    private val _logger: ILogger,
    private val _stateTtlMs: Long = 10 * 60 * 1000L,
) : IClaimResponseManager {

    private enum class Step { CHECK_PENDING, SIGN_PENDING }

    private class PendingState(
        val correlationId: String,
        val uid: Int,
        val userName: String,
        val blockRewardType: BLOCK_REWARD_TYPE,
        val tokenTypeInBlockChain: Int,
        val dataType: DataType,
        val dataTypeName: String,
        val controllerRef: WeakReference<IUserController>,
        val createdAtMs: Long,
        @Volatile var step: Step,
        @Volatile var newTotalClaimed: Double? = null,
        @Volatile var giftDetails: List<String>? = null,
    )

    private val _pending = ConcurrentHashMap<String, PendingState>()

    private val tagCheck = "[${StreamKeys.AP_SIG_CLAIM_CHECK_RESPONSE_STR}]"
    private val tagSign = "[${StreamKeys.AP_SIG_CLAIM_SIGN_RESPONSE_STR}]"

    override fun beginCheck(
        controller: IUserController,
        walletAddress: String,
        blockRewardType: BLOCK_REWARD_TYPE,
        tokenTypeInBlockChain: Int,
    ): String {
        sweepExpired()
        val correlationId = UUID.randomUUID().toString()
        val state = PendingState(
            correlationId = correlationId,
            uid = controller.userId,
            userName = walletAddress,
            blockRewardType = blockRewardType,
            tokenTypeInBlockChain = tokenTypeInBlockChain,
            dataType = controller.dataType,
            dataTypeName = controller.dataType.name.lowercase(),
            controllerRef = WeakReference(controller),
            createdAtMs = System.currentTimeMillis(),
            step = Step.CHECK_PENDING,
        )
        _pending[correlationId] = state

        _coroutine.scope.launch(Dispatchers.IO) {
            try {
                val url = String.format(
                    _envManager.apSignatureCmdCheckTotalClaimedUrlV4,
                    state.userName,
                    state.tokenTypeInBlockChain,
                    state.dataTypeName,
                    state.correlationId,
                )
                _restApi.get(url)
                _logger.log("$tagCheck Fired check request cid=${state.correlationId} uid=${state.uid}")
            } catch (e: Exception) {
                _logger.error("$tagCheck Failed to fire check request cid=${state.correlationId}", e)
                _pending.remove(state.correlationId)
            }
        }
        return correlationId
    }

    override fun listenClaimCheck(message: String): Boolean {
        try {
            _logger.log("$tagCheck Received: $message")
            val response = JsonExtensionBuilder.json.decodeFromString<ClaimCheckResponse>(message)
            val state = getActive(response.correlationId) ?: run {
                _logger.log("$tagCheck No pending state for cid=${response.correlationId}, dropping")
                return true
            }
            if (state.step != Step.CHECK_PENDING) {
                _logger.log("$tagCheck cid=${response.correlationId} already advanced to ${state.step}, dropping")
                return true
            }

            if (response.errorMessage != null) {
                _logger.log("$tagCheck Upstream error cid=${response.correlationId}: ${response.errorMessage}")
                _pending.remove(response.correlationId)
                pushErrorToClient(state, response.errorMessage)
                return true
            }

            val controller = state.controllerRef.get()
            if (controller == null || _usersManager.getUserController(state.uid, state.dataType, EnumConstants.Landing.TREASURE) !== controller) {
                _logger.log("$tagCheck User ${state.uid} offline, dropping cid=${response.correlationId}")
                _pending.remove(response.correlationId)
                return true
            }

            val finalize = controller.masterUserManager.claimManager.finalizeClaimAfterCheck(
                state.blockRewardType,
                response.totalClaimed,
            )
            state.newTotalClaimed = finalize.newTotalClaimed
            state.giftDetails = finalize.giftDetails
            state.step = Step.SIGN_PENDING

            fireSignRequest(state)
            return true
        } catch (e: Exception) {
            _logger.error("$tagCheck Error processing message: ${e.message}", e)
            return false
        }
    }

    override fun listenClaimSign(message: String): Boolean {
        try {
            _logger.log("$tagSign Received: $message")
            val response = JsonExtensionBuilder.json.decodeFromString<ClaimSignResponse>(message)
            val state = _pending.remove(response.correlationId)
            if (state == null) {
                _logger.log("$tagSign No pending state for cid=${response.correlationId}, dropping")
                return true
            }

            if (response.errorMessage != null) {
                _logger.log("$tagSign Upstream error cid=${response.correlationId}: ${response.errorMessage}")
                pushErrorToClient(state, response.errorMessage)
                return true
            }

            val controller = state.controllerRef.get()
            if (controller == null || _usersManager.getUserController(state.uid, state.dataType, EnumConstants.Landing.TREASURE) !== controller) {
                _logger.log("$tagSign User ${state.uid} offline, dropping cid=${response.correlationId}")
                return true
            }

            val giftDetails = state.giftDetails ?: emptyList()
            val result = controller.masterUserManager.claimManager.buildClaimV4SfsObject(
                state.tokenTypeInBlockChain,
                giftDetails,
                response.nonce,
                response.signature,
                response.amount,
            )
            controller.sendDataEncryption(HandlerCommand.ApproveClaimResponse, result, true)
            _logger.log("$tagSign Pushed ApproveClaimResponse uid=${state.uid} cid=${response.correlationId}")
            return true
        } catch (e: Exception) {
            _logger.error("$tagSign Error processing message: ${e.message}", e)
            return false
        }
    }

    private fun pushErrorToClient(state: PendingState, errorMessage: String) {
        val controller = state.controllerRef.get()
        if (controller == null || _usersManager.getUserController(state.uid, state.dataType, EnumConstants.Landing.TREASURE) !== controller) {
            _logger.log("$tagCheck User ${state.uid} offline, skip error push cid=${state.correlationId}")
            return
        }
        val result = SFSObject()
        result.putInt("code", 100)
        result.putUtfString("message", errorMessage)
        controller.sendDataEncryption(HandlerCommand.ApproveClaimResponse, result, true)
        _logger.log("$tagCheck Pushed error ApproveClaimResponse uid=${state.uid} cid=${state.correlationId}")
    }

    private fun fireSignRequest(state: PendingState) {
        _coroutine.scope.launch(Dispatchers.IO) {
            try {
                val url = String.format(
                    _envManager.apSignatureCmdClaimRewardUrlV4,
                    state.dataTypeName,
                    state.correlationId,
                )
                val body = buildJsonObject {
                    put("userAddress", state.userName)
                    put("tokenType", state.tokenTypeInBlockChain)
                    put("newTotalClaimed", state.newTotalClaimed ?: 0.0)
                    put("network", state.dataTypeName)
                    put("giftDetails", Json.parseToJsonElement(
                        Json.encodeToString(state.giftDetails ?: emptyList())
                    ))
                }
                _restApi.post(url, _envManager.apSignatureToken, body)
                _logger.log("$tagSign Fired sign request cid=${state.correlationId} uid=${state.uid}")
            } catch (e: Exception) {
                _logger.error("$tagSign Failed to fire sign request cid=${state.correlationId}", e)
                _pending.remove(state.correlationId)
            }
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
            if (isExpired(iterator.next().value)) iterator.remove()
        }
    }

    private fun isExpired(state: PendingState): Boolean {
        return System.currentTimeMillis() - state.createdAtMs > _stateTtlMs
    }

    override fun initialize() {}
}
