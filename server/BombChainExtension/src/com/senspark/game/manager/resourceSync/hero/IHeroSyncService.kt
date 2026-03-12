package com.senspark.game.manager.resourceSync.hero

import com.senspark.game.api.BlockchainHeroResponse
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.declare.EnumConstants.DataType
import java.util.concurrent.ConcurrentHashMap

interface IHeroSyncService {
    fun syncHero(mediator: UserControllerMediator, currentHero: ConcurrentHashMap<Int, Hero>, dataSync: List<BlockchainHeroResponse>): HeroSyncData
    fun syncHeroOffline(uid: Int, dataType: DataType, dataSync: List<BlockchainHeroResponse>)
    fun updateStakeAmountHeroes(hero: Hero, stakeBcoin: Double, stakeSen: Double)
}