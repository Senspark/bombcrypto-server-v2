package com.senspark.game.manager.ton

import com.senspark.common.utils.ILogger
import com.senspark.game.api.IRestApi
import com.senspark.game.api.OkHttpRestApi
import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.treassureHunt.ICoinRankingManager
import com.senspark.game.data.model.user.IUserInfo
import com.senspark.game.db.IRewardDataAccess
import com.senspark.game.db.ITHModeDataAccess
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.customEnum.ChangeRewardReason
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.IEnvManager
import com.senspark.game.utils.deserialize
import com.senspark.game.utils.HashIdGenerator
import com.senspark.lib.data.manager.IGameConfigManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.Serializable
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class MemberInfoResponse(
    val id: Int,
    var claimable: Double,
    val hasParent: Boolean,
    var childrenTotalAmount: Int,
)

@Serializable
data class ClaimableResponse(
    val id: Int,
    val claimed: Double
)

private const val MAX_LENGTH = 20

class TonReferralManager(
    private val thModeDataAccess: ITHModeDataAccess,
    private val rewardDataAccess: IRewardDataAccess,
    private val envManager: IEnvManager,
    private val coinRankingManager: ICoinRankingManager,
    private val userClubManager: IClubManager,
    private val logger: ILogger,
    private val gameConfigManager: IGameConfigManager,
    private val hashIdGenerator: HashIdGenerator,
) : IReferralManager {
    private val _api: IRestApi = OkHttpRestApi()

    // cache lại, chỉ gọi để update vào 1 khoảng thời gian trong ngày để coi như phát thưởng
    private val _dataMemberInfo: ConcurrentHashMap<Int, Pair<MemberInfoResponse, Long>> =
        ConcurrentHashMap(mutableMapOf())

    private var _configReferralParams: MutableMap<String, Int> = mutableMapOf()

    override fun initialize() {
        _configReferralParams = thModeDataAccess.getReferralParamsConfig()
    }

    override fun setConfig(configReferralParams: MutableMap<String, Int>) {
        _configReferralParams = configReferralParams
    }

    override fun getReferral(uid: Int): ISFSObject {
        // tính xem đến thời gian phát thưởng chưa, đến rồi thì gọi api để update
        val timePayOutConfig = gameConfigManager.timePayOutReferral

        val nextTimePayOut = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val currentHour = nextTimePayOut.get(Calendar.HOUR_OF_DAY)
        var nextHourPayOut = if (timePayOutConfig != 0) ((currentHour / timePayOutConfig) + 1) * timePayOutConfig else 0

        if (nextHourPayOut >= 24) {
            nextHourPayOut %= 24
            nextTimePayOut.add(Calendar.DAY_OF_YEAR, 1)
        }
        nextTimePayOut.set(Calendar.HOUR_OF_DAY, nextHourPayOut)
        nextTimePayOut.set(Calendar.MINUTE, 0)
        nextTimePayOut.set(Calendar.SECOND, 0)
        nextTimePayOut.set(Calendar.MILLISECOND, 0)

        val memberInfoResponse: MemberInfoResponse
        val result = SFSObject()
        result.putUtfString("referral_code", hashIdGenerator.encode(uid))
        result.putInt("min_claim_referral", gameConfigManager.minClaimReferral)
        result.putInt("time_pay_out_referral", gameConfigManager.timePayOutReferral)

        if (_dataMemberInfo.containsKey(uid) && nextTimePayOut.timeInMillis <= _dataMemberInfo[uid]!!.second) {
            memberInfoResponse = _dataMemberInfo[uid]!!.first
        } else {
            val url = String.format(envManager.getMemberInfoUrl, uid)
            val bodyJson: String
            try {
                bodyJson = _api.get(url, envManager.apSignatureToken)
                memberInfoResponse = deserialize<MemberInfoResponse>(bodyJson)

                // nếu timePayOutConfig bằng 0 thì không add để luôn gọi api
                if (timePayOutConfig != 0) {
                    _dataMemberInfo[uid] = Pair(memberInfoResponse, nextTimePayOut.timeInMillis)
                } else {
                    _dataMemberInfo.remove((uid))
                }
            } catch (e: Exception) {
                logger.error(e)
                result.putInt("child_quantity", 0)
                result.putDouble("rewards", 0.0)
                return result
            }
        }

        result.putInt("child_quantity", memberInfoResponse.childrenTotalAmount)
        result.putDouble("rewards", memberInfoResponse.claimable)
        return result
    }

    override fun createReferral(userInfo: IUserInfo, referralCode: String) {
        try {
            val mapReferral = parseReferralCode(referralCode)

            if (mapReferral.containsKey("c") && userInfo.newUser) {
                // Giới hạn kí tự tránh attacker chèn mã độc
                if (mapReferral["c"]!!.length > MAX_LENGTH) {
                    return
                }
                val parentId = hashIdGenerator.decode(mapReferral["c"]!!)
                val url = String.format(envManager.addChildUrl, parentId, userInfo.id, "")
                _api.get(url, envManager.apSignatureToken)
                // Nếu api không lỗi thì thêm trực tiếp số lượng con trên server
                if (_dataMemberInfo.containsKey(parentId)) {
                    _dataMemberInfo[parentId]!!.first.childrenTotalAmount += 1
                }
            } else if (mapReferral.containsKey("n")) {
                // Giới hạn kí tự tránh attacker chèn mã độc
                val identifierName = mapReferral["n"]!!
                if (identifierName.length > MAX_LENGTH) {
                    return
                }
                if (_configReferralParams.containsKey(identifierName)) {
                    val parentId = _configReferralParams[identifierName]
                    val url = String.format(envManager.addChildUrl, parentId, userInfo.id, identifierName)
                    _api.get(url, envManager.apSignatureToken)
                }
            }
            if (mapReferral.containsKey("k")) {
                // Giới hạn kí tự tránh attacker chèn mã độc
                if (mapReferral["k"]!!.length > MAX_LENGTH) {
                    return
                }
                val idTelegram = mapReferral["k"]!!.toLong()
                val clubId = userClubManager.getClubIdByTelegramId(idTelegram);
                if(clubId == null) {
                    logger.error("Club id not found for id telegram ${idTelegram}")
                    return;
                }
                userClubManager.joinClub(userInfo.id, userInfo.displayName, clubId, false)
            }
        } catch (e: Exception) {
            logger.error(e)
        }
    }

    override fun addEarning(uid: Int, addition: Double) {
        try {
            val url = String.format(envManager.addEarningUrl, uid, addition)
            _api.get(url, envManager.apSignatureToken)
        } catch (e: Exception) {
            logger.error(e)
        }
    }

    override fun claimRewards(userController: IUserController) {
        if (!gameConfigManager.enableClaimReferral) {
            throw CustomException("This feature will coming soon")
        }
        val url = String.format(envManager.claimReferralUrl, userController.userId)
        try {
            val bodyJson = _api.get(url, envManager.apSignatureToken)
            val claimableResponse = deserialize<ClaimableResponse>(bodyJson)
            val reward = claimableResponse.claimed.toFloat()
            coinRankingManager.saveRankingCoin(
                userController.userId,
                reward,
                userController.dataType
            )
            rewardDataAccess.addUserBlockReward(
                userController.userId,
                BLOCK_REWARD_TYPE.COIN,
                userController.dataType.getCoinType(),
                reward,
                0f,
                ChangeRewardReason.CLAIM_REFERRAL_TON
            )
            // claim xong rồi thì lưu log và update lại cache
            thModeDataAccess.logClaimReferral(userController.userId, reward)
            _dataMemberInfo[userController.userId]?.first?.claimable = 0.0
        } catch (e: Exception) {
            throw CustomException("Claim Failed")
        }
    }

    private fun parseReferralCode(input: String): MutableMap<String, String> {
        val result = mutableMapOf<String, String>()
        val parts = input.split("-")

        for (i in parts.indices step 2) {
            if (i + 1 < parts.size) {
                result[parts[i]] = parts[i + 1]
            }
        }

        return result
    }
}
