package com.senspark.game.data.manager.hero

import com.senspark.common.service.IServerService
import com.senspark.game.data.model.config.HeroRepairShield

interface IHeroRepairShieldDataManager: IServerService {
    fun getPrice(rarity: Int, shieldLevel: Int): HeroRepairShield
    fun getPrice(rarity: Int): HeroRepairShield
    fun getPriceConfig() : Map<Int, Map<Int, HeroRepairShield>>
} 