package com.senspark.game.data.manager.hero

import com.senspark.common.service.IServerService
import com.senspark.common.utils.ILogger
import com.senspark.game.data.model.config.HeroUpgradePower
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.declare.SFSField
import com.senspark.lib.data.manager.BaseDataManager
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

interface IHeroUpgradePowerManager : IServerService {
    fun toSFSObject(): ISFSObject
    fun initialize(hash: Map<Int, HeroUpgradePower>)
    fun getPowerIncrease(rare: Int, level: Int): Int
}

class HeroUpgradePowerManager(
    private val _shopDataAccess: IShopDataAccess,
    logger: ILogger,
) : BaseDataManager<Int, HeroUpgradePower>(logger), IHeroUpgradePowerManager {

    private val hash: MutableMap<Int, HeroUpgradePower> = mutableMapOf()

    private var _data: ISFSObject? = null

    override fun initialize() {
        hash.putAll(_shopDataAccess.loadHeroUpgradePower())
        initialize(hash)
    }

    override fun initialize(hash: Map<Int, HeroUpgradePower>) {
        super.initialize(hash)
        _data = null
    }

    private val data
        get(): ISFSObject {
            if (_data != null) {
                return _data as ISFSObject
            }

            _data = SFSObject()
            val sfsArr: ISFSArray = SFSArray()
            _data!!.putSFSArray(SFSField.Datas, sfsArr)

            val upgradeCostLst: List<HeroUpgradePower> = list()
            for (upgradeCost in upgradeCostLst) {
                val rareObj = SFSObject()
                sfsArr.addSFSObject(rareObj)
                rareObj.putInt(SFSField.Rare, upgradeCost.rare)
                val lvlPower: ISFSArray = SFSArray()
                rareObj.putSFSArray(SFSField.Power, lvlPower)

                upgradeCost.powers.forEach { lvlPower.addInt(it) }
            }
            return _data as ISFSObject
        }

    override fun toSFSObject(): ISFSObject {
        return data
    }

    override fun getPowerIncrease(rare: Int, level: Int): Int {
        // Uniform scaling for Hero Evolution V2 (Rarity-independent)
        // Scaling Principle: L2-L5 (+1/lvl), L6-L9 (+2/lvl), L10 (+3)
        return when {
            level <= 1 -> 0
            level <= 5 -> level - 1           // L2:1, L3:2, L4:3, L5:4
            level <= 9 -> 4 + (level - 5) * 2 // L6:6, L7:8, L8:10, L9:12
            level >= 10 -> 15                 // L10:15
            else -> 0
        }
    }
}
