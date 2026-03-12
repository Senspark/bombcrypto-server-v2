package com.senspark.game.data.manager.hero

import com.senspark.game.data.model.config.HeroUpgradePower
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class NullHeroUpgradePowerManager : IHeroUpgradePowerManager {
    override fun initialize() {
    }
    
    override fun initialize(hash: Map<Int, HeroUpgradePower>) {}

    override fun toSFSObject(): ISFSObject {
        return SFSObject()
    }

    override fun getPowerIncrease(rare: Int, level: Int): Int {
        return 0
    }
}
