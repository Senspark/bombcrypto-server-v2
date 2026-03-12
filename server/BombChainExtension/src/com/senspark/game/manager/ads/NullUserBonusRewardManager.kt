package com.senspark.game.manager.ads

import com.senspark.game.data.RewardData
import com.senspark.game.declare.EnumConstants.DeviceType
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class NullUserBonusRewardManager : IUserBonusRewardManager {
    override fun addRewardsAds(rewardId: String, reward: RewardData?) {}

    override suspend fun takeBonusReward(rewardId: String, adsToken: String): List<RewardData> {
        return emptyList()
    }

    override suspend fun takeLuckyWheelReward(
        rewardId: String,
        adsToken: String,
        deviceType: DeviceType
    ): ISFSObject {
        return SFSObject()
    }
}
