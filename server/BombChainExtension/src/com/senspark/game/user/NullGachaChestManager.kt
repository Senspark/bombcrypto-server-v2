package com.senspark.game.user

import com.senspark.game.data.model.config.IGachaChest
import com.senspark.game.declare.customEnum.GachaChestType
import com.senspark.game.exception.CustomException

class NullGachaChestManager : IGachaChestManager {
    override val chestList get() = emptyList<IGachaChest>()

    override fun initialize() {
    }
    
    override fun getChest(chestType: GachaChestType): IGachaChest {
        throw CustomException("Feature not support")
    }

    override fun getChestShop(chestType: GachaChestType): IGachaChest {
        throw CustomException("Feature not support")
    }
}

