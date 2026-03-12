package com.senspark.game.manager.config

import com.senspark.game.db.model.UserFreeRewardConfig
import com.senspark.game.db.model.UserGachaChestSlot
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import java.time.Instant

interface IUserConfigManager {
    val userGachaChestSlots: List<UserGachaChestSlot>
    val numberChestSlot: Int
    val freeRewardConfig: UserFreeRewardConfig
    val miscConfigs: MiscConfigs
    var cachedPurchasedPacks: MutableList<String>?
    val noAds: Boolean
    val isReceivedFirstChestSkipTime: Boolean
    val isReceivedTutorialReward: Boolean
    val totalCostumePresetSlot: Int
    val lastTimeClaimSubscription: Instant?
    fun buyGachaChestSlot(slot: Int)
    fun changeUserFreeRewardOpenTimeConfigToNow(rewardType: BLOCK_REWARD_TYPE)
    fun saveMiscConfigs()
    fun reloadConfig()
    fun setNoAds()
    fun setReceivedFirstChestSkipTime()
    fun buyCostumePresetSlot(rewardType: BLOCK_REWARD_TYPE)
}