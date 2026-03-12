package com.senspark.game.manager.hero

import com.senspark.game.constant.ItemStatus
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.customEnum.UpgradeHeroType
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject

interface IUserHeroTRManager {
    val heroes: Map<Int, Hero>
    val heroesMapByType: MutableMap<EnumConstants.HeroTRType, MutableMap<Int, ItemHero>>
    val heroesMapByItemId: Map<Int, ItemHero>
    val heroesSoulMapByItemId: Map<Int, ItemHero>
    fun isHavingHero(id: Int): Boolean
    fun canGetFreeHeroTR(): Boolean
    fun getHero(id: Int): Hero
    fun loadHero(loadImmediately: Boolean = false)
    fun upgradeHero(heroId: Int, upgradeType: UpgradeHeroType): ISFSObject
    fun grindHero(itemId: Int, quantity: Int, itemStatus: ItemStatus): ISFSArray
    fun active(id: Int)
} 