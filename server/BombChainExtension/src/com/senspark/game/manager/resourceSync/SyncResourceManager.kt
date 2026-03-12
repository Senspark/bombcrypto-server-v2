package com.senspark.game.manager.resourceSync

import com.senspark.game.data.manager.hero.IConfigHeroTraditionalManager
import com.senspark.game.data.manager.hero.IHeroBuilder
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.db.IGameDataAccess
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.manager.resourceSync.hero.IHeroSyncService
import com.senspark.game.manager.resourceSync.hero.HeroSyncService
import com.senspark.game.manager.resourceSync.house.IHouseSyncService
import com.senspark.game.manager.resourceSync.house.HouseSyncService
import com.senspark.game.manager.stake.IHeroStakeManager
import com.senspark.game.service.IAllHeroesFiManager
import com.senspark.lib.data.manager.IGameConfigManager

class SyncResourceManager(
    dataAccessManager: IDataAccessManager,
    gameDataAccess: IGameDataAccess,
    userDataAccess: IUserDataAccess,
    gameConfigManager: IGameConfigManager,

    heroStakeManager: IHeroStakeManager,
    heroBuilder: IHeroBuilder,
    allHeroFiManager: IAllHeroesFiManager,
    traditionalManager: IConfigHeroTraditionalManager
) : ISyncResourceManager {

    override val houseSyncService: IHouseSyncService = HouseSyncService(dataAccessManager, gameDataAccess)
    override val heroSyncService: IHeroSyncService = HeroSyncService(
        gameDataAccess,
        userDataAccess,
        gameConfigManager,

        heroStakeManager,
        heroBuilder,
        allHeroFiManager,
        traditionalManager
    )

    override fun initialize() {
    }

}