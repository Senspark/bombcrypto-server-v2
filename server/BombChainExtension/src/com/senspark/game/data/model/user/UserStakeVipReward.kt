package com.senspark.game.data.model.user

import com.senspark.game.declare.EnumConstants.StakeVipRewardType
import com.senspark.game.declare.ErrorCode
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.SFSObject
import java.util.*

class UserStakeVipReward(
    val level: Int,
    val rewardType: StakeVipRewardType,
    val type: String,
    private val quantity: Int,
    var havingQuantity: Int,
    val nextClaim: Long
) {
    fun toSfsObject(): SFSObject {
        if (enoughTimeClaim()) {
            havingQuantity = quantity
        }
        val sfsObject = SFSObject()
        sfsObject.putInt("level", level)
        sfsObject.putUtfString("rewardType", rewardType.name)
        sfsObject.putUtfString("type", type)
        sfsObject.putInt("quantity", quantity)
        sfsObject.putInt("havingQuantity", havingQuantity)
        sfsObject.putLong("nextClaim", nextClaim)
        return sfsObject
    }

    fun canClaim(throwException: Boolean = true): Boolean {
        val enoughTime = enoughTimeClaim()
        if (havingQuantity == 0 && !enoughTime) {
            if (throwException) throw CustomException(
                "Reward not enough time",
                ErrorCode.SERVER_ERROR
            ) else return false
        } else if (enoughTime) {
            havingQuantity = quantity
        }
        return true
    }

    fun updateQuantityIfEnoughClaimTime() {
        if (havingQuantity == 0 && enoughTimeClaim()) {
            havingQuantity = quantity
        }
    }

    private fun enoughTimeClaim(): Boolean {
        val nextTimeClaim = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        nextTimeClaim.timeInMillis = nextClaim
        val nowInUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        return nextTimeClaim <= nowInUtc
    }
}