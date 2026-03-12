package com.senspark.game.manager.rock

import com.senspark.game.controller.IUserController
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.exception.CustomException

class NullUserRockManager : IUserRockManager {

    override val convertHeroRockConfig: Map<Int, RockAmount> get() = emptyMap()

    override fun initialize() {
    }

    override fun setConfig(convertHeroRockConfig: Map<Int, RockAmount>) {}

    override fun upgradeShieldLevel(userController: IUserController, hero: Hero): Pair<Int, String> {
        throw CustomException("Feature not support")
    }

    override fun createRock(
        userController: IUserController,
        tx: String,
        walletAddress: String,
        listIdHero: List<Int>
    ): Float {
        return 0f
    }
}