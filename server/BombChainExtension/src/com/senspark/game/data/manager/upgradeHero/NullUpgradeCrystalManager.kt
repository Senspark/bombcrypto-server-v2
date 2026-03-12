package com.senspark.game.data.manager.upgradeHero

import com.senspark.game.data.model.config.UpgradeCrystal
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray

class NullUpgradeCrystalManager : IUpgradeCrystalManager {

    override fun initialize() {
    }

    override fun toSfsArray(): ISFSArray {
        return SFSArray()
    }

    override fun getBySourceItemId(itemId: Int): UpgradeCrystal {
        throw CustomException("Feature not support")
    }
}