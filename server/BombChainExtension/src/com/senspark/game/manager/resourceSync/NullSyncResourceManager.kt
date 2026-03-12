package com.senspark.game.manager.resourceSync

import com.senspark.game.manager.resourceSync.hero.IHeroSyncService
import com.senspark.game.manager.resourceSync.hero.NullHeroSyncService
import com.senspark.game.manager.resourceSync.house.IHouseSyncService
import com.senspark.game.manager.resourceSync.house.NullHouseSyncService

class NullSyncResourceManager : ISyncResourceManager {
    override val houseSyncService: IHouseSyncService = NullHouseSyncService()
    override val heroSyncService: IHeroSyncService = NullHeroSyncService()
    override fun initialize() {
    }
}