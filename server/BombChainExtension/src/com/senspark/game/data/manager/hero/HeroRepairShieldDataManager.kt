package com.senspark.game.data.manager.hero

import com.senspark.game.data.model.config.HeroRepairShield
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.declare.ErrorCode
import com.senspark.game.exception.CustomException

class HeroRepairShieldDataManager(
    private val _shopDataAccess: IShopDataAccess,
) : IHeroRepairShieldDataManager {

    private val _repairPrice: MutableMap<Int, Map<Int, HeroRepairShield>> = mutableMapOf()

    override fun initialize() {
        _repairPrice.putAll(_shopDataAccess.loadHeroRepairShield())
    }
    
    override fun getPrice(rarity: Int, shieldLevel: Int): HeroRepairShield {
        return _repairPrice[rarity]?.get(shieldLevel) ?: throw CustomException(
            "Price for [$rarity,$shieldLevel]",
            ErrorCode.SERVER_ERROR
        )
    }

    override fun getPrice(rarity: Int): HeroRepairShield {
        return getPrice(rarity, 0)
    }

    override fun getPriceConfig(): Map<Int, Map<Int, HeroRepairShield>> {
        return _repairPrice
    }
}