package com.senspark.game.service

import com.senspark.game.data.HeroUpgradeShieldData

class NullHeroUpgradeShieldManager : IHeroUpgradeShieldManager {
    override fun initialize() {
        
    }
    
    override fun getValue(level: Int, rarity: Int): Int {
        return 0
    }

    override fun getPrice(level: Int, rarity: Int): Float {
        return 0f
    }

    override fun getData(): List<HeroUpgradeShieldData> {
        return emptyList()
    }
}