package com.senspark.game.data.manager.mysteryBox

import com.senspark.common.service.IServerService
import com.senspark.game.data.model.config.IMysteryBox

interface IMysteryBoxManager : IServerService {
    fun getRandomItem(): IMysteryBox
} 