package com.senspark.game.manager.treasureHuntV2

import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.user.RewardDetail

data class UserHero(
    val userId: UserId,
    val hero: Hero,
    /**
     * Số vé
     */
    var ticketCount: Int = 1
)

data class RewardResult(
    val userHero: UserHero,
    val rewardLevel: Int,
    val reward: RewardDetail,
)

data class MultipleRewardResult(
    val hero: Hero,
    val rewardLevel: Int,
    /**
     * Gồm: BCoin & SEN & Coin
     */
    val reward: List<RewardDetail>,
)