package com.senspark.game.manager.hero

import com.senspark.game.constant.ItemStatus
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.declare.EnumConstants.HeroTRType
import com.senspark.game.declare.customEnum.UpgradeHeroType
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class NullUserHeroTRManager : IUserHeroTRManager {
    override fun loadHero(loadImmediately: Boolean) {}

    override val heroes: Map<Int, Hero> = mutableMapOf()

    override val heroesMapByType: MutableMap<HeroTRType, MutableMap<Int, ItemHero>> = mutableMapOf()

    override val heroesMapByItemId: Map<Int, ItemHero> = emptyMap()

    override val heroesSoulMapByItemId: Map<Int, ItemHero> = emptyMap()
    override fun getHero(id: Int): Hero {
        throw CustomException("Feature not support")
    }

    override fun active(id: Int) {}

    override fun canGetFreeHeroTR(): Boolean {
        return true
    }

    override fun grindHero(itemId: Int, quantity: Int, itemStatus: ItemStatus): ISFSArray {
        return SFSArray()
    }

    override fun upgradeHero(heroId: Int, upgradeType: UpgradeHeroType): ISFSObject {
        return SFSObject()
    }

    override fun isHavingHero(id: Int): Boolean {
        return true
    }
}