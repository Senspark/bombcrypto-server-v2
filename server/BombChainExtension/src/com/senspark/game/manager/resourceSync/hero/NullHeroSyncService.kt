package com.senspark.game.manager.resourceSync.hero

import com.senspark.game.api.BlockchainHeroResponse
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.declare.EnumConstants
import com.senspark.game.exception.CustomException
import java.util.concurrent.ConcurrentHashMap

class NullHeroSyncService : IHeroSyncService {
    override fun syncHero(
        mediator: UserControllerMediator,
        currentHero: ConcurrentHashMap<Int, Hero>,
        dataSync: List<BlockchainHeroResponse>
    ): HeroSyncData {
        throw CustomException("Sync feature not support airdrop user")
    }

    override fun syncHeroOffline(
        uid: Int,
        dataType: EnumConstants.DataType,
        dataSync: List<BlockchainHeroResponse>
    ) {
        throw CustomException("Sync feature not support airdrop user")
    }

    override fun updateStakeAmountHeroes(
        hero: Hero,
        stakeBcoin: Double,
        stakeSen: Double
    ) {
        throw CustomException("Sync feature not support airdrop user")
    }
}