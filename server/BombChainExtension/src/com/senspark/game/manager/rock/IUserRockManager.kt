package com.senspark.game.manager.rock

import com.senspark.common.service.IServerService
import com.senspark.game.controller.IUserController
import com.senspark.game.data.model.nft.Hero

interface IUserRockManager : IServerService {
    val convertHeroRockConfig: Map<Int, RockAmount>
    fun setConfig(convertHeroRockConfig: Map<Int, RockAmount>)
    fun upgradeShieldLevel(userController: IUserController, hero: Hero): Pair<Int, String>
    fun createRock(userController: IUserController, tx: String, walletAddress: String, listIdHero: List<Int>): Float
}