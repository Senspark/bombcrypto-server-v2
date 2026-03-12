package com.senspark.game.data.manager.upgradeHero

import com.senspark.common.utils.toSFSArray
import com.senspark.game.data.model.config.UpgradeCrystal
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.ISFSArray

class UpgradeCrystalManager(
    private val _shopDataAccess: IShopDataAccess,
) : IUpgradeCrystalManager {

    private val _configs: MutableMap<Int, UpgradeCrystal> = mutableMapOf()

    override fun initialize() {
        _configs.putAll(_shopDataAccess.loadUpgradeCrystalConfig())
    }

    override fun toSfsArray(): ISFSArray {
        return _configs.toSFSArray {
            it.value.toSfsObject()
        }
    }

    override fun getBySourceItemId(itemId: Int): UpgradeCrystal {
        return _configs[itemId] ?: throw CustomException("Crystal with itemId $itemId not allow upgrade")
    }
}