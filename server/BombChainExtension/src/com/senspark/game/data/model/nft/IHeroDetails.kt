package com.senspark.game.data.model.nft

import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.declare.EnumConstants.HeroTRType

interface IHeroDetails {
    val details: String
    val heroId: Int
    val index: Int
    val rarity: Int
    val level: Int
    val color: Int
    val skin: Int
    val stamina: Int
    val speed: Int
    val bombSkin: Int
    val bombCount: Int
    val bombPower: Int
    val bombRange: Int
    val hp: Int
    val dmg: Int
    val abilityList: HeroAbilityList
    val abilityHeroSList: HeroAbilityList
    val resetShieldCounter: Int
    val shieldLevel: Int
    val type: EnumConstants.HeroType
    val maxSpeed: Int
    val maxRange: Int
    val maxBomb: Int
    val maxHp: Int
    val maxDmg: Int
    val maxUpgradeSpeed: Int
    val maxUpgradeRange: Int
    val maxUpgradeBomb: Int
    val maxUpgradeHp: Int
    val maxUpgradeDmg: Int
    val dataType: DataType
    val heroTRType: HeroTRType
    val heroConfig: ConfigHeroTraditional?
    val upgradedSpeed: Int
    val upgradedRange: Int
    val upgradedBomb: Int
    val upgradedHp: Int
    val upgradedDmg: Int

    fun isEqualTo(other: IHeroDetails): Boolean
    fun isHeroS():Boolean
}