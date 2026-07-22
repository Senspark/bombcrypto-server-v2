package com.senspark.game.service

import com.senspark.common.service.IServerService
import com.senspark.game.data.HeroUpgradeShieldData
import com.senspark.game.db.IShopDataAccess

interface IHeroUpgradeShieldManager : IServerService {
    fun getValue(level: Int, rarity: Int): Int
    fun getPrice(level: Int, rarity: Int): Float
    fun getData(): List<HeroUpgradeShieldData>
}

class HeroUpgradeShieldManager(
    private val shopDataAccess: IShopDataAccess,
) : IHeroUpgradeShieldManager {
    private var _data: List<HeroUpgradeShieldData> = emptyList()

    override fun initialize() {
        _data = shopDataAccess.queryHeroUpgradeShield()
        val raritiesPresent = _data.map { it.rarity }.toSet()
        val missing = (0..9).filterNot { it in raritiesPresent }
        check(missing.isEmpty()) {
            "config_hero_upgrade_shield missing rarities: $missing. Apply rarity 6-9 migration."
        }
    }

    override fun getValue(level: Int, rarity: Int): Int {
        return _data[rarity].values[level]
    }

    override fun getPrice(level: Int, rarity: Int): Float {
        return _data[rarity].prices[level]
    }

    override fun getData(): List<HeroUpgradeShieldData> {
        return _data
    }
}