package com.senspark.game.user

import com.senspark.common.constant.PvPItemType
import com.senspark.game.constant.Booster
import com.senspark.game.data.model.nft.Hero
import com.smartfoxserver.v2.entities.data.ISFSObject

interface IAdventureHero {
    val hero: Hero
    val dmg: Int
    val speed: Int
    val hp: Int
    val bomb: Int
    val range: Int
    
    val heroId: Int
    fun subHealth(value: Int)
    fun applyBooster(boosters: Set<Booster>)
    fun takeItem(item: PvPItemType)
    fun toSFSObject(): ISFSObject
    fun canPierceBlock(): Boolean
    fun revive()
}