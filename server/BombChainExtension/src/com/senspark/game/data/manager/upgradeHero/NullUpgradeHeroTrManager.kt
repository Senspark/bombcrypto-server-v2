package com.senspark.game.data.manager.upgradeHero

import com.senspark.game.data.model.config.UpgradeHeroTr
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.declare.customEnum.UpgradeHeroType
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray

class NullUpgradeHeroTrManager : IUpgradeHeroTrManager {

    override fun initialize() {
    }

    override fun toSfsArray(): ISFSArray {
        return SFSArray()
    }

    override fun get(type: UpgradeHeroType, hero: Hero): UpgradeHeroTr {
        throw CustomException("Feature not support")
    }
}