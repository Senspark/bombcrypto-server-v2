package com.senspark.game.user

import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.data.model.config.IGachaChest
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.declare.customEnum.GachaChestType
import com.senspark.game.exception.CustomException

class GachaChestManager(
    private val _shopDataAccess: IShopDataAccess,
    private val _configItemManager: IConfigItemManager,
) : IGachaChestManager {
    
    companion object {
        const val SKIP_TIME_PER_ADS_IN_MILLIS: Long = 30 * 60 * 1000
    }
    
    private val _chestMap: MutableMap<GachaChestType, IGachaChest> = mutableMapOf()
    override val chestList: MutableList<IGachaChest> = mutableListOf()

    override fun initialize() {
        _chestMap.putAll(_shopDataAccess.loadGachaChestConfigs(_configItemManager))
        chestList.addAll(_chestMap.values.toList().sortedBy { it.type.value })
    }

    override fun getChest(chestType: GachaChestType): IGachaChest {
        return _chestMap[chestType] ?: throw CustomException("Cannot not find ${chestType.name} chest")
    }

    override fun getChestShop(chestType: GachaChestType): IGachaChest {
        val chest = getChest(chestType)
        if (!chest.isSellable) {
            throw CustomException("Shop not sell ${chestType.name} chest")
        }
        return chest
    }
}

