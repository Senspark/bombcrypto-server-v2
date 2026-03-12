package com.senspark.game.api

import com.senspark.common.utils.AppStage
import com.senspark.common.utils.ILogger
import com.senspark.game.manager.IEnvManager
import com.senspark.game.manager.IUsersManager
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Serializable
data class AdRewardData(
    val userId: String,
    val timestamp: Long,
    val verified: Boolean,
    val duration: Int
)

/** Representing steps in the verification process */
enum class VerificationStep {
    CLIENT, 
    REDIS
}

/** Represents the state of a verification step */
enum class VerificationState {
    PENDING, // Not set yet
    TRUE,    // Verified successfully
    FALSE    // Verification failed
}

/**
 * Represents verification status with both required steps
 * @property clientState Current verification state from client-side
 * @property redisState Current verification state from Redis
 * @property createdAt Timestamp when this verification status was created
 */
data class VerificationStatus(
    val clientState: VerificationState = VerificationState.PENDING,
    val redisState: VerificationState = VerificationState.PENDING,
    val createdAt: Long = System.currentTimeMillis()
) {
        
    val isBothTrue: Boolean
        get() = clientState == VerificationState.TRUE && redisState == VerificationState.TRUE
        
    val hasFalse: Boolean
        get() = (clientState == VerificationState.FALSE && redisState != VerificationState.PENDING)
            || (redisState == VerificationState.FALSE && clientState != VerificationState.PENDING)
    
    val hasClientConfirm: Boolean
        get() = clientState != VerificationState.PENDING
        
    /**
     * Updates the status with a new state for a specific verification step
     * @param step The verification step to update
     * @param state The new state value
     * @return A new VerificationStatus with the updated step
     */
    fun withStep(step: VerificationStep, state: VerificationState): VerificationStatus {
        return when (step) {
            VerificationStep.CLIENT -> copy(clientState = state)
            VerificationStep.REDIS -> copy(redisState = state)
        }
    }
}

/**
 * Manages verification of advertisement tokens through a two-step process:
 * 1. Client verification
 * 2. Redis verification
 * 
 * Both steps must be successfully completed for a token to be considered valid.
 */
class VerifyAdApiManager(
    envManager: IEnvManager,
    private val _logger: ILogger
) : IVerifyAdApiManager {
    
    
    companion object {
        private val TOKEN_TIMEOUT_DURATION = 5.minutes.inWholeMilliseconds
    }
    
    private val _isServerTest = envManager.appStage != AppStage.PROD 
    private val _pendingTokens = ConcurrentHashMap<String, VerificationStatus>()
    private val _json = Json { ignoreUnknownKeys = true }

    override fun initialize() {
    }

    /**
     * Validates an advertisement token from client side
     * @param token The token to validate
     * @return True if validation was accepted, false otherwise
     */
    override suspend fun isValidAds(token: String?): Boolean {
        
//        if (_isServerTest) {
//            _logger.log("[AD VERIFICATION] Test server: auto-approving ads token")
//            // For test server, auto-approve all tokens
//            if (!token.isNullOrBlank()) {
//                updateTokenStatus(token, VerificationStep.CLIENT, VerificationState.TRUE)
//                _logger.log("[AD VERIFICATION] Auto-approved token: ${token.take(6)}... in test mode")
//            }
//            return true
//        }
        
        if (token.isNullOrBlank()) {
            _logger.error("Invalid token: null or blank")
            return false
        }
        
        try {
            // Mark the client step as TRUE
            updateTokenStatus(token, VerificationStep.CLIENT, VerificationState.TRUE)

            var timeoutCounter = 0
            val maxTimeout = (TOKEN_TIMEOUT_DURATION / 1000).toInt() // Convert to seconds

            // Store the original token to track if it's been removed/processed
            val originalToken = token

            while (timeoutCounter < maxTimeout) {
                val status = _pendingTokens[originalToken]

                when {
                    status == null -> {
                        // Token was removed, verification process completed
                        break
                    }
                    status.isBothTrue -> {
                        _logger.log("[AD VERIFICATION] Token $token VERIFIED SUCCESSFULLY (both steps TRUE)")
                        return true
                    }
                    status.hasFalse -> {
                        _logger.log("[AD VERIFICATION] Token $token VERIFICATION FAILED (has FALSE state)")
                        return false
                    }
                    else -> {
                        delay(1000)
                        timeoutCounter++
                    }
                }
            }
            
            // Timeout
            _logger.log("[AD VERIFICATION] Token $token TIMED OUT after ${timeoutCounter}s")
            return false
            
        } catch (e: Exception) {
            _logger.error("Error validating ads token: ${e.message}")
            updateTokenStatus(token, VerificationStep.CLIENT, VerificationState.FALSE)
            return false
        }
    }

    /**
     * Processes advertisement reward data from Redis
     * @param json The JSON string containing ad reward data
     */
    override fun processAdsReward(json: String) {
        try {
            val adData = _json.decodeFromString<AdRewardData>(json)
            val userId = adData.userId
            val verificationState = if (adData.verified) VerificationState.TRUE else VerificationState.FALSE
            
            updateTokenStatus(userId, VerificationStep.REDIS, verificationState)
        } catch (e: Exception) {
            _logger.error("Failed to process ad reward: ${e.message}")
            
            // Try to extract userId from the json to mark as FALSE
            tryExtractUserIdAndMarkFailed(json)
        }
    }
    
    /**
     * Attempts to extract a userId from malformed JSON and mark the token as failed
     */
    private fun tryExtractUserIdAndMarkFailed(json: String) {
        try {
            val adData = _json.decodeFromString<AdRewardData>(json)
            updateTokenStatus(adData.userId, VerificationStep.REDIS, VerificationState.FALSE)
        } catch (parseError: Exception) {
            _logger.error("Could not extract userId from invalid JSON: ${parseError.message}")
        }
    }
    
    /**
     * Updates the status of a token for a specific verification step
     */
    private fun updateTokenStatus(token: String, step: VerificationStep, state: VerificationState) {
        val currentStatus = _pendingTokens.getOrPut(token) { VerificationStatus() }
        _pendingTokens[token] = currentStatus.withStep(step, state)
    }
}