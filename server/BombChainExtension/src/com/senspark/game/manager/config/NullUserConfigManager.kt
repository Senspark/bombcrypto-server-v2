package com.senspark.game.manager.config

import com.senspark.game.db.model.UserGachaChestSlot
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.exception.CustomException
import java.time.Instant

class NullUserConfigManager : IUserConfigManager {
    override val userGachaChestSlots = mutableListOf<UserGachaChestSlot>()
    override val numberChestSlot = 0
    override val freeRewardConfig get() = throw CustomException("Feature not support")
    override val miscConfigs get() = throw CustomException("Feature not support")
    override var cachedPurchasedPacks: MutableList<String>? = null
    override val lastTimeClaimSubscription = null
    override val noAds: Boolean = true
    override val isReceivedFirstChestSkipTime = true
    override val isReceivedTutorialReward = true
    override val totalCostumePresetSlot = 0

    override fun reloadConfig() {}

    override fun buyGachaChestSlot(slot: Int) {}

    override fun buyCostumePresetSlot(rewardType: BLOCK_REWARD_TYPE) {}

    override fun setNoAds() {}

    override fun setReceivedFirstChestSkipTime() {}

    override fun changeUserFreeRewardOpenTimeConfigToNow(rewardType: BLOCK_REWARD_TYPE) {}

    override fun saveMiscConfigs() {}
}