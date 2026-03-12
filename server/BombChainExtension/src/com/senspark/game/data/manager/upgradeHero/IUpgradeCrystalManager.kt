package com.senspark.game.data.manager.upgradeHero

import com.senspark.common.service.IServerService
import com.senspark.game.data.model.config.UpgradeCrystal
import com.smartfoxserver.v2.entities.data.ISFSArray

interface IUpgradeCrystalManager : IServerService {
    fun getBySourceItemId(itemId: Int): UpgradeCrystal
    fun toSfsArray(): ISFSArray
}