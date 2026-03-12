package com.senspark.game.data.manager.gacha

import com.senspark.game.data.model.config.GachaChestSlot
import com.senspark.game.db.IShopDataAccess

class GachaChestSlotManager(
    val shopDataAccess: IShopDataAccess,
) : IGachaChestSlotManager {
    override val slots: MutableMap<Int, GachaChestSlot> = mutableMapOf()

    override fun initialize() {
        slots.putAll(shopDataAccess.loadGachaChestSlots())
    }
} 