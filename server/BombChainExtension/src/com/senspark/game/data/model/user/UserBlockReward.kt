package com.senspark.game.data.model.user

import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.ErrorCode
import com.senspark.game.exception.CustomException
import com.senspark.lib.data.manager.GameConfigManager
import java.time.Instant

class UserBlockReward {
    val rewardType: BLOCK_REWARD_TYPE
    @Volatile var values: Float = 0.0f
    var totalValues: Double = 0.0
    var lastTimeClaimSuccess: Long = Instant.now().epochSecond
    var claimPending: Double = 0.0
    var dataType: EnumConstants.DataType = EnumConstants.DataType.TR

    constructor(rewardType:BLOCK_REWARD_TYPE) : this(rewardType, EnumConstants.DataType.TR)

    constructor(rewardType: BLOCK_REWARD_TYPE, dataType: EnumConstants.DataType) {
        this.rewardType = rewardType
        this.values = 0.0f
        this.totalValues = 0.0
        this.lastTimeClaimSuccess = Instant.now().epochSecond
        this.claimPending = 0.0
        this.dataType = dataType
    }

    @Synchronized
    fun addValues(values: Float) {
        this.values += values
        this.totalValues += values
    }

    @Synchronized
    fun deductValues(values: Float) {
        this.values -= values
    }

    fun getRemainTimeCanClaim(nextTimeCanClaimReward: Int): Int {
        val currTime = System.currentTimeMillis()
        val nextTimeCanClaim = lastTimeClaimSuccess + nextTimeCanClaimReward * 60 * 1000 //m * 60s * 1000ms

        val remainTime = nextTimeCanClaim - currTime
        return if (remainTime <= 0) {
            0
        } else {
            (remainTime / 1000).toInt() //return second
        }
    }

    @Throws(CustomException::class)
    fun canClaim(claimBcoinLimit:List<List<Int>>): Boolean {
        if (claimPending > 0) {
            return true
        }
        return when (rewardType) {
            BLOCK_REWARD_TYPE.SENSPARK -> values >= 40
            BLOCK_REWARD_TYPE.BCOIN -> {
                val result = values > 0 && claimBcoinLimit.any { e ->
                    val minvalue = e[0]
                    val maxValue = e[1]
                    ((minvalue >= 0 && values >= minvalue) || minvalue < 0)
                        && ((maxValue >= 0 && values <= maxValue) || maxValue < 0)
                }
                if (result) {
                    true
                } else {
                    val message = claimBcoinLimit.joinToString(" or ") { e ->
                        val minvalue = e[0]
                        val maxValue = e[1]
                        when {
                            minvalue >= 0 && maxValue >= 0 -> "$minvalue <= claim value <= $maxValue"
                            minvalue > 0 -> "claim value >= $minvalue"
                            else -> "claim value <= $maxValue"
                        }
                    }
                    throw CustomException("Claim condition: $message", ErrorCode.NOT_ENOUGH_REWARD)
                }
            }
            BLOCK_REWARD_TYPE.BCOIN_DEPOSITED, BLOCK_REWARD_TYPE.BOMBERMAN -> values > 0
            else -> values > 1
        }
    }
}
