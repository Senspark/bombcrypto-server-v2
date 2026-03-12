package com.senspark.game.data.model.config

import com.senspark.game.db.model.GachaChestSlotType

class GachaChestSlot(
    val slot: Int,
    val type: GachaChestSlotType,
    val price: Int
)