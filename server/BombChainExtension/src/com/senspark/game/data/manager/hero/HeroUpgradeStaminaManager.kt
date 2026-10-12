package com.senspark.game.data.manager.hero

import com.senspark.common.service.IServerService
import com.senspark.common.utils.ILogger
import com.senspark.game.data.model.config.HeroUpgradeStamina
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.declare.SFSField
import com.senspark.lib.data.manager.BaseDataManager
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

interface IHeroUpgradeStaminaManager : IServerService {
    fun toSFSObject(): ISFSObject
    fun initialize(hash: Map<Int, HeroUpgradeStamina>)
    fun getStaminaIncrease(rare: Int, level: Int): Int
}

/**
 * Gemeo de [HeroUpgradePowerManager] para o ganho de stamina dos niveis 6-10.
 *
 * Fica num manager proprio, e nao num campo extra do de power, porque as duas tabelas evoluem
 * separadamente: os niveis pares dao energia e os impares dao poder.
 */
class HeroUpgradeStaminaManager(
    private val _shopDataAccess: IShopDataAccess,
    logger: ILogger,
) : BaseDataManager<Int, HeroUpgradeStamina>(logger), IHeroUpgradeStaminaManager {

    private val hash: MutableMap<Int, HeroUpgradeStamina> = mutableMapOf()

    private var _data: ISFSObject? = null

    override fun initialize() {
        hash.putAll(_shopDataAccess.loadHeroUpgradeStamina())
        initialize(hash)
    }

    override fun initialize(hash: Map<Int, HeroUpgradeStamina>) {
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

            val upgradeLst: List<HeroUpgradeStamina> = list()
            for (upgrade in upgradeLst) {
                val rareObj = SFSObject()
                sfsArr.addSFSObject(rareObj)
                rareObj.putInt(SFSField.Rare, upgrade.rare)
                val lvlStamina: ISFSArray = SFSArray()
                rareObj.putSFSArray(SFSField.Stamina, lvlStamina)

                upgrade.staminas.forEach { lvlStamina.addInt(it) }
            }
            return _data as ISFSObject
        }

    override fun toSFSObject(): ISFSObject {
        return data
    }

    override fun getStaminaIncrease(rare: Int, level: Int): Int {
        val stamina: HeroUpgradeStamina = get(rare) ?: return 0
        val staminas: List<Int> = stamina.staminas

        // Level comeca em 1, entao subtrai 1 para indexar.
        return if ((level - 1) < staminas.size) {
            staminas[level - 1]
        } else {
            0
        }
    }
}
