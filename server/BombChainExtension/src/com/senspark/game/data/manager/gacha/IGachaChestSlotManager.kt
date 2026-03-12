package com.senspark.game.data.manager.gacha

import com.senspark.common.service.IServerService
import com.senspark.game.data.model.config.GachaChestSlot

interface IGachaChestSlotManager : IServerService {
    val slots: Map<Int, GachaChestSlot>
}