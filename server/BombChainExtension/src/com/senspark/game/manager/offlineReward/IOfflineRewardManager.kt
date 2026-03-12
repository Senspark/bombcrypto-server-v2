package com.senspark.game.manager.offlineReward

import com.senspark.common.service.IServerService
import com.senspark.game.controller.IUserController

interface IOfflineRewardManager : IServerService {
    fun getRewards(userController: IUserController): Pair<Int, Map<ItemId, Int>>
    fun claimRewards(userController: IUserController): Pair<Int, Map<ItemId, Int>>
    suspend fun claimRewardsWithAds(userController: IUserController, adsToken: String): Pair<OfflineHours, Map<ItemId, Int>>
}