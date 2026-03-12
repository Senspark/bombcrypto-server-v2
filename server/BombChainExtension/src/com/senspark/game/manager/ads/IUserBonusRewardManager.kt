package com.senspark.game.manager.ads

import com.senspark.game.data.RewardData
import com.senspark.game.declare.EnumConstants.DeviceType
import com.smartfoxserver.v2.entities.data.ISFSObject

interface IUserBonusRewardManager {
    fun addRewardsAds(rewardId: String, reward: RewardData? = null)
    suspend fun takeBonusReward(rewardId: String, adsToken: String): List<RewardData>

    /**
     * take lucky wheel reward by view ads (mobile), pay gold (web)
     *
     * @param adsToken token ads
     * @param noAds bypass validate adsToken
     */
    suspend fun takeLuckyWheelReward(rewardId: String, adsToken: String, deviceType: DeviceType): ISFSObject
}
