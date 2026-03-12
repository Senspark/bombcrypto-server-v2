package com.senspark.game.db.model

import com.smartfoxserver.v2.entities.data.SFSObject

class UserStakeInfo(
//    Số stake hiện tại
    val principal: Double,
//    Lợi nhuận hiện tại
    val profit: Double,
//    ngày stake
    val stakeDate: Long,
//    phí withdraw
    val withdrawFee: Double,
//    Tổng lượn stake của toàn bộ user
    val totalStake: Double,
//    lãi suất theo ngày
    val APD: Double,
//    số tiền nhận được sau with draw
    val receiveAmount: Double,
)

class UserStakeVipReward(
    val type: String,
    private val rewardType: String,
    private val quantity: Int
) {
    fun toSfsObject(): SFSObject {
        val sfsObject = SFSObject()
        sfsObject.putUtfString("type", type)
        sfsObject.putUtfString("rewardType", rewardType)
        sfsObject.putInt("quantity", quantity)

        return sfsObject
    }
}