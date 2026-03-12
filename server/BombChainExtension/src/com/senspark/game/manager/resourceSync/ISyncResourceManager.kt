package com.senspark.game.manager.resourceSync

import com.senspark.common.service.IServerService
import com.senspark.game.manager.resourceSync.hero.IHeroSyncService
import com.senspark.game.manager.resourceSync.house.IHouseSyncService

interface ISyncResourceManager: IServerService {
    val houseSyncService: IHouseSyncService
    val heroSyncService: IHeroSyncService
}