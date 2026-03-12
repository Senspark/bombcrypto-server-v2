package com.senspark.game.data.manager.upgradeHero

import com.senspark.common.service.IServerService
import com.senspark.game.data.model.config.UpgradeHeroTr
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.declare.customEnum.UpgradeHeroType
import com.smartfoxserver.v2.entities.data.ISFSArray

interface IUpgradeHeroTrManager : IServerService {
    fun get(type: UpgradeHeroType, hero: Hero): UpgradeHeroTr
    fun toSfsArray(): ISFSArray
} 