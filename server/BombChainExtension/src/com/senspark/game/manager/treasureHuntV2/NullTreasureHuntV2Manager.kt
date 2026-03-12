package com.senspark.game.manager.treasureHuntV2

import com.senspark.game.controller.IUserController
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.user.RewardDetail
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray

class NullTreasureHuntV2Manager : ITreasureHuntV2Manager {
    override var period = 60

    override fun initialize() {
    }

    override fun setStopPool(stop: Boolean) {}

    override fun reloadConfigs() {}

    override fun joinRoom(user: IUserController) {}

    override fun leaveRoom(user: IUserController) {}

    override fun addHeroToPool(hero: Hero, userId: UserId): List<Int> {
        return emptyList()
    }

    override fun calculateReward(): Map<UserId, List<MultipleRewardResult>> {
        return emptyMap()
    }

    override fun sumReward(
        rewardList: List<MultipleRewardResult>
    ): Map<BLOCK_REWARD_TYPE, RewardDetail> {
        return emptyMap()
    }

    override fun getRewardDetail(rewardList: List<MultipleRewardResult>): Map<BLOCK_REWARD_TYPE, Map<Int, List<RewardDetail>>> {
        return emptyMap()
    }

    override fun saveRewardPool() {}

    override fun refillRewardPool() {}

    override fun allPoolToSFSArray(): ISFSArray {
        return SFSArray()
    }
}