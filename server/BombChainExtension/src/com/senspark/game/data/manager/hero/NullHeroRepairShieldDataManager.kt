package com.senspark.game.data.manager.hero

import com.senspark.game.data.model.config.HeroRepairShield
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.declare.ErrorCode
import com.senspark.game.exception.CustomException

class NullHeroRepairShieldDataManager : IHeroRepairShieldDataManager {

    override fun initialize() {
    }

    override fun getPrice(rarity: Int, shieldLevel: Int): HeroRepairShield {
        throw CustomException("Feature not support")
    }

    override fun getPrice(rarity: Int): HeroRepairShield {
        throw CustomException("Feature not support")
    }

    override fun getPriceConfig(): Map<Int, Map<Int, HeroRepairShield>> {
        return emptyMap()
    }
}