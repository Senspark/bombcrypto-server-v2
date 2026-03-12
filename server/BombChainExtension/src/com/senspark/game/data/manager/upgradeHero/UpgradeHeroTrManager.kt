package com.senspark.game.data.manager.upgradeHero

import com.senspark.common.utils.toSFSArray
import com.senspark.game.data.model.config.UpgradeHeroTr
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.declare.customEnum.ConfigUpgradeHeroType
import com.senspark.game.declare.customEnum.UpgradeHeroType
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class UpgradeHeroTrManager(
    private val _shopDataAccess: IShopDataAccess,
) : IUpgradeHeroTrManager {

    private val _configs: MutableMap<ConfigUpgradeHeroType, Map<Int, UpgradeHeroTr>> = mutableMapOf()

    override fun initialize() {
        _configs.putAll(_shopDataAccess.loadUpgradeHeroTrConfig())
    }

    override fun toSfsArray(): ISFSArray {
        return _configs.toSFSArray {
            SFSObject().apply {
                putUtfString("upgrade_type", it.key.name)
                putSFSArray("fee",
                    it.value.toSFSArray { it2 ->
                        it2.value.toSfsObject()
                    }
                )
            }
        }
    }

    override fun get(type: UpgradeHeroType, hero: Hero): UpgradeHeroTr {
        val currentValue = when (type) {
            UpgradeHeroType.DMG -> hero.dmg
            UpgradeHeroType.HP -> hero.hp
            UpgradeHeroType.RANGE -> hero.upgradedRange
            UpgradeHeroType.SPEED -> hero.upgradedSpeed
            UpgradeHeroType.BOMB -> hero.upgradedBomb
        }
        return _configs[type.type]?.get(currentValue + 1) ?: throw CustomException("Cannot upgrade")
    }
}