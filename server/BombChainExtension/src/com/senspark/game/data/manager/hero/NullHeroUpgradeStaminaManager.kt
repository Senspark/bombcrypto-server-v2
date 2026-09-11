package com.senspark.game.data.manager.hero

import com.senspark.game.data.model.config.HeroUpgradeStamina
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class NullHeroUpgradeStaminaManager : IHeroUpgradeStaminaManager {
    override fun initialize() {
    }

    override fun initialize(hash: Map<Int, HeroUpgradeStamina>) {}

    override fun toSFSObject(): ISFSObject {
        return SFSObject()
    }

    override fun getStaminaIncrease(rare: Int, level: Int): Int {
        return 0
    }
}
