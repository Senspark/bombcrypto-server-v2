package com.senspark.game.manager.treasureHuntV2

import com.senspark.common.service.IServerService
import com.senspark.game.controller.IUserController
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.user.RewardDetail
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.smartfoxserver.v2.entities.data.ISFSArray

interface ITreasureHuntV2Manager : IServerService {
    val period: Int
    fun reloadConfigs()
    fun joinRoom(user: IUserController)
    fun leaveRoom(user: IUserController)
    fun addHeroToPool(hero: Hero, userId: UserId): List<Int>
    fun calculateReward(): Map<UserId, List<MultipleRewardResult>>
    fun sumReward(rewardList: List<MultipleRewardResult>): Map<BLOCK_REWARD_TYPE, RewardDetail>
    fun getRewardDetail(rewardList: List<MultipleRewardResult>): Map<BLOCK_REWARD_TYPE, Map<Int, List<RewardDetail>>>
    fun saveRewardPool()
    fun refillRewardPool()
    fun allPoolToSFSArray(): ISFSArray
    fun setStopPool(stop: Boolean)
}