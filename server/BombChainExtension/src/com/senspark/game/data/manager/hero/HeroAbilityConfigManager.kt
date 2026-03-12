package com.senspark.game.data.manager.hero

import com.senspark.game.data.model.config.HeroAbilityConfig
import com.senspark.game.db.IShopDataAccess

class HeroAbilityConfigManager(
    private val _shopDataAccess: IShopDataAccess,
) : IHeroAbilityConfigManager {

    private val _data: MutableMap<Int, HeroAbilityConfig> = mutableMapOf()

    override fun initialize() {
        _data.putAll(_shopDataAccess.loadBomberAbility())
    }

    override fun getConfig(ability: Int): HeroAbilityConfig {
        return _data[ability] ?: throw IllegalArgumentException("Invalid ability: $ability")
    }
}