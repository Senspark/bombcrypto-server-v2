package com.senspark.game.data.manager.gacha

import com.senspark.game.data.model.config.GachaChestSlot

class NullGachaChestSlotManager : IGachaChestSlotManager {
    override val slots: Map<Int, GachaChestSlot> get() = emptyMap()
    
    override fun initialize() {
    }
} 