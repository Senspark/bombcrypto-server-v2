package com.senspark.game.manager.claim

import com.senspark.game.api.HttpClient.Companion.getInstance
import com.senspark.game.api.model.request.ClaimSignatureRequest
import com.senspark.game.api.model.response.ClaimSignatureResponse
import com.senspark.game.api.model.response.ClaimSignatureResponseData
import com.senspark.game.api.model.response.TotalClaimResponse
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.model.user.UserBlockReward
import com.senspark.game.data.model.user.UserBlockRewardGift
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE.*
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSField
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.IEnvManager
import com.senspark.game.manager.blockReward.IUserBlockRewardManager
import com.senspark.game.manager.claim.dto.ClaimV4Finalize
import com.senspark.game.manager.claim.dto.ClaimV4Prep
import com.senspark.game.utils.Utils
import com.senspark.game.utils.deserialize
import com.senspark.game.utils.deserializeList
import com.senspark.lib.data.manager.IGameConfigManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.*

class ClaimManagerPolygon(
    private val _mediator: UserControllerMediator,
    private val blockRewardManager: IUserBlockRewardManager,
) : IClaimManager {

    private val envManager = _mediator.services.get<IEnvManager>()
    private val dataAccessManager = _mediator.services.get<IDataAccessManager>()
    private val gameConfigManager = _mediator.services.get<IGameConfigManager>()

    override fun claimReward(blockRewardType: BLOCK_REWARD_TYPE): ISFSObject {
        val userBlockReward = blockRewardManager.get(blockRewardType) ?: throw CustomException(
            "User block reward not exists"
        )

        val rewardType = userBlockReward.rewardType

        if (!gameConfigManager.enableClaimToken && (rewardType == BCOIN || rewardType == SENSPARK)) {
            throw CustomException("This feature will be coming soon.")
        }
        if (!gameConfigManager.enableClaimTokenDeposited && (rewardType == BCOIN_DEPOSITED || rewardType == SENSPARK_DEPOSITED)) {
            throw CustomException("This feature will be coming soon.")
        }
        if (!gameConfigManager.enableClaimHero && rewardType == BOMBERMAN) {
            throw CustomException("This feature will be coming soon.")
        }

//        mở claim hero
        if (rewardType != BOMBERMAN && isSameDay(userBlockReward)) {
            throw CustomException("You can only withdraw once a day(UTC time).", ErrorCode.NOT_EXECUTE)
        }

        return when (rewardType) {
            BCOIN, SENSPARK, BCOIN_DEPOSITED, SENSPARK_DEPOSITED, WOFM, BOMBERMAN -> claim(userBlockReward)
            else -> throw CustomException("Invalid token ${rewardType.name}")
        }
    }


    @Throws(java.lang.Exception::class)
    fun claim(userBlockReward: UserBlockReward): ISFSObject {
        val claimBcoinLimit = gameConfigManager.claimBcoinLimit
        if (!userBlockReward.canClaim(claimBcoinLimit)) {
            throw CustomException("Not enough reward", ErrorCode.NOT_ENOUGH_REWARD)
        }
        val blockRewardType = userBlockReward.rewardType
        val tokenTypeInBlockChain = tokenTypeInBlockChain(blockRewardType)
        val totalClaimed = callGetTotalClaimed(tokenTypeInBlockChain)
        val jsonResult = dataAccessManager.rewardDataAccess.saveUserClaimRewardData(
            _mediator.userId,
            _mediator.dataType,
            blockRewardType,
            getMinClaim(blockRewardType),
            totalClaimed,
        )
        val newTotalClaimed = jsonResult["value"]?.jsonPrimitive?.double ?: throw CustomException("Missing value")
        val received = jsonResult["received"]?.jsonPrimitive?.double ?: throw CustomException("Missing value")
        val gifts = deserializeList<UserBlockRewardGift>(jsonResult["gifts"]?.jsonArray.toString())
        val giftDetails: MutableList<String> = ArrayList()
        if (blockRewardType == BOMBERMAN) {
            gifts.forEach { giftDetails.add(it.parseToGenId().toString()) }
            val remainItemCount = (received - gifts.sumOf { it.count }).toInt()
            for (i in 0 until remainItemCount) {
                giftDetails.add(0.toString())
            }
        }
        val response = callGetSignature(tokenTypeInBlockChain, newTotalClaimed, giftDetails)
        blockRewardManager.loadUserBlockReward()
        val result: ISFSObject = SFSObject()
        result.putSFSArray(SFSField.Rewards, blockRewardManager.toSfsArrays())
        result.putInt("nonce", response.nonce)
        result.putUtfString("signature", response.signature)
        result.putDouble("amount", response.amount.toDouble())
        result.putInt("tokenType", tokenTypeInBlockChain)
        result.putUtfStringArray("details", giftDetails)
        return result
    }

    override fun confirmClaimSuccess(blockRewardType: BLOCK_REWARD_TYPE): ISFSObject {
        val userBlockReward = blockRewardManager.get(blockRewardType) ?: throw CustomException(
            "User block reward not exists",
            ErrorCode.SERVER_ERROR
        )
        return confirmClaimSuccess(userBlockReward)
    }

    @Throws(Exception::class)
    fun confirmClaimSuccess(
        userBlockReward: UserBlockReward
    ): ISFSObject {
        val blockRewardType = userBlockReward.rewardType
        val tokenTypeInBlockChain: Int = tokenTypeInBlockChain(blockRewardType)
        val totalClaimed: Double = callGetTotalClaimed(tokenTypeInBlockChain)
        val received: Double = dataAccessManager.rewardDataAccess.syncUserClaimSynced(
            _mediator.userId,
            _mediator.dataType,
            blockRewardType,
            totalClaimed,
        )
        blockRewardManager.loadUserBlockReward()
        val result: ISFSObject = SFSObject()
        result.putDouble("received", received)
        result.putSFSArray(SFSField.Rewards, blockRewardManager.toSfsArrays())
        return result
    }

    private fun getMinClaim(blockRewardType: BLOCK_REWARD_TYPE): Float {
        return when (blockRewardType) {
            BCOIN, SENSPARK -> 40f
            BOMBERMAN, BCOIN_DEPOSITED, SENSPARK_DEPOSITED -> 1f
            else -> throw CustomException("Invalid blockRewardType", ErrorCode.SERVER_ERROR)
        }
    }

    @Throws(java.lang.Exception::class)
    private fun callGetTotalClaimed(tokenTypeInBlockChain: Int): Double {
        val url = java.lang.String.format(
            envManager.apSignatureCmdCheckTotalClaimedUrl,
            _mediator.walletAddress,
            tokenTypeInBlockChain,
            _mediator.dataType.name.lowercase()
        )
        val request: Request = Request.Builder()
            .url(url)
            .method("GET", null)
            .addHeader("Content-Type", "application/json")
            .build()
        getInstance().newCall(request).execute().use { response ->
            return if (response.isSuccessful && response.body != null) {
                deserialize<TotalClaimResponse>(response.body!!.string()).message.toDouble()
            } else {
                throw CustomException(
                    String.format("Call get total claimed failed,code %s", response.code),
                    ErrorCode.SERVER_ERROR
                )
            }
        }
    }

    @Throws(java.lang.Exception::class)
    private fun callGetSignature(
        tokenTypeInBlockChain: Int,
        newTotalClaimed: Double,
        giftDetails: List<String>
    ): ClaimSignatureResponseData {
        val url = java.lang.String.format(
            envManager.apSignatureCmdClaimRewardUrl,
            _mediator.walletAddress,
            tokenTypeInBlockChain,
            newTotalClaimed,
            _mediator.dataType.name.lowercase()
        )
        val body = ClaimSignatureRequest(
            _mediator.walletAddress!!,
            tokenTypeInBlockChain,
            newTotalClaimed,
            giftDetails,
            _mediator.dataType.name.lowercase()
        )
        val mediaType: MediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody: RequestBody = Json.encodeToString(body).toRequestBody(mediaType)
        val request: Request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer ${envManager.apSignatureToken}")
            .build()
        getInstance().newCall(request).execute().use { response ->
            return if (response.isSuccessful && response.body != null) {
                deserialize<ClaimSignatureResponse>(response.body!!.string()).message
            } else {
                throw CustomException(
                    String.format("Call get signature,code %s", response.code),
                    ErrorCode.SERVER_ERROR
                )
            }
        }
    }


    override fun prepareClaimV4(blockRewardType: BLOCK_REWARD_TYPE): ClaimV4Prep {
        val userBlockReward = blockRewardManager.get(blockRewardType)
            ?: throw CustomException("User block reward not exists")

        val rewardType = userBlockReward.rewardType

        if (!gameConfigManager.enableClaimToken && (rewardType == BCOIN || rewardType == SENSPARK)) {
            throw CustomException("This feature will be coming soon.")
        }
        if (!gameConfigManager.enableClaimTokenDeposited && (rewardType == BCOIN_DEPOSITED || rewardType == SENSPARK_DEPOSITED)) {
            throw CustomException("This feature will be coming soon.")
        }
        if (!gameConfigManager.enableClaimHero && rewardType == BOMBERMAN) {
            throw CustomException("This feature will be coming soon.")
        }
        if (rewardType != BOMBERMAN && isSameDay(userBlockReward)) {
            throw CustomException("You can only withdraw once a day(UTC time).", ErrorCode.NOT_EXECUTE)
        }
        when (rewardType) {
            BCOIN, SENSPARK, BCOIN_DEPOSITED, SENSPARK_DEPOSITED, WOFM, BOMBERMAN -> Unit
            else -> throw CustomException("Invalid token ${rewardType.name}")
        }
        val claimBcoinLimit = gameConfigManager.claimBcoinLimit
        if (!userBlockReward.canClaim(claimBcoinLimit)) {
            throw CustomException("Not enough reward", ErrorCode.NOT_ENOUGH_REWARD)
        }
        return ClaimV4Prep(tokenTypeInBlockChain(rewardType))
    }

    override fun finalizeClaimAfterCheck(
        blockRewardType: BLOCK_REWARD_TYPE,
        totalClaimed: Double,
    ): ClaimV4Finalize {
        val jsonResult = dataAccessManager.rewardDataAccess.saveUserClaimRewardData(
            _mediator.userId,
            _mediator.dataType,
            blockRewardType,
            getMinClaim(blockRewardType),
            totalClaimed,
        )
        val newTotalClaimed = jsonResult["value"]?.jsonPrimitive?.double
            ?: throw CustomException("Missing value")
        val received = jsonResult["received"]?.jsonPrimitive?.double
            ?: throw CustomException("Missing value")
        val gifts = deserializeList<UserBlockRewardGift>(jsonResult["gifts"]?.jsonArray.toString())
        val giftDetails: MutableList<String> = ArrayList()
        if (blockRewardType == BOMBERMAN) {
            gifts.forEach { giftDetails.add(it.parseToGenId().toString()) }
            val remainItemCount = (received - gifts.sumOf { it.count }).toInt()
            for (i in 0 until remainItemCount) {
                giftDetails.add(0.toString())
            }
        }
        return ClaimV4Finalize(newTotalClaimed, giftDetails, received)
    }

    override fun syncPendingClaimsFromChain() {
        if (_mediator.walletAddress.isNullOrEmpty()) {
            _mediator.logger.log("[syncPendingClaimsFromChain] skip — no wallet address")
            return
        }

        val claimableTypes = listOf(BCOIN, SENSPARK, BCOIN_DEPOSITED, SENSPARK_DEPOSITED, BOMBERMAN)
        val pendingByType = claimableTypes.mapNotNull { type ->
            blockRewardManager.get(type)?.takeIf { it.claimPending > 0 }?.let { type to it.claimPending }
        }.toMap()
        if (pendingByType.isEmpty()) {
            _mediator.logger.log("[syncPendingClaimsFromChain] skip — no pending claims")
            return
        }

        _mediator.logger.log("[syncPendingClaimsFromChain] start dataType=${_mediator.dataType} pending=$pendingByType")

        var successCount = 0
        for ((rewardType, pending) in pendingByType) {
            try {
                val tokenType = tokenTypeInBlockChain(rewardType)
                val totalClaimed = callGetTotalClaimed(tokenType)
                dataAccessManager.rewardDataAccess.syncUserClaimSynced(
                    _mediator.userId,
                    _mediator.dataType,
                    rewardType,
                    totalClaimed,
                )
                successCount++
                _mediator.logger.log("[syncPendingClaimsFromChain] $rewardType OK totalClaimed=$totalClaimed pending=$pending")
            } catch (e: Exception) {
                _mediator.logger.error("[syncPendingClaimsFromChain] $rewardType failed", e)
            }
        }

        blockRewardManager.loadUserBlockReward()
        _mediator.logger.log("[syncPendingClaimsFromChain] done success=$successCount/${pendingByType.size}")
    }

    override fun buildClaimV4SfsObject(
        tokenTypeInBlockChain: Int,
        giftDetails: List<String>,
        nonce: Int,
        signature: String,
        amount: Double,
    ): ISFSObject {
        blockRewardManager.loadUserBlockReward()
        val result: ISFSObject = SFSObject()
        result.putSFSArray(SFSField.Rewards, blockRewardManager.toSfsArrays())
        result.putInt("nonce", nonce)
        result.putUtfString("signature", signature)
        result.putDouble("amount", amount)
        result.putInt("tokenType", tokenTypeInBlockChain)
        result.putUtfStringArray("details", giftDetails)
        return result
    }

    @Throws(CustomException::class)
    private fun tokenTypeInBlockChain(blockRewardType: BLOCK_REWARD_TYPE): Int {
        return when (blockRewardType) {
            BCOIN -> 0
            BOMBERMAN -> 1
            BCOIN_DEPOSITED -> 2
            SENSPARK -> 3
            SENSPARK_DEPOSITED -> 4
            else -> throw CustomException("Unsupported reward type", ErrorCode.INVALID_PARAMETER)
        }
    }

    private fun isSameDay(userBlockReward: UserBlockReward): Boolean {
        val currentTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val timeClaimSuccessOfDay = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        timeClaimSuccessOfDay.timeInMillis = userBlockReward.lastTimeClaimSuccess
        return Utils.compare2DateOfYear(currentTime, timeClaimSuccessOfDay)
    }
}