package com.senspark.game.manager.offlineReward

import com.senspark.game.controller.IUserController
import com.senspark.game.exception.CustomException

class NullOfflineRewardManager : IOfflineRewardManager {

    override fun initialize() {
    }

    override fun claimRewards(userController: IUserController): Pair<Int, Map<ItemId, Int>> {
        throw CustomException("Feature not support")
    }

    override suspend fun claimRewardsWithAds(
        userController: IUserController,
        adsToken: String
    ): Pair<OfflineHours, Map<ItemId, Int>> {
        throw CustomException("Feature not support")
    }

    override fun getRewards(userController: IUserController): Pair<OfflineHours, Map<ItemId, Int>> {
        throw CustomException("Feature not support")
    }
}
