package com.senspark.game.data.model.nft

import com.senspark.game.data.manager.hero.IConfigHeroTraditionalManager
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.declare.EnumConstants.HeroTRType
import com.senspark.game.utils.DetailsExtractor
import com.smartfoxserver.v2.entities.data.ISFSObject

class NonFiHeroDetails(
    obj: ISFSObject,
    dataType: DataType,
    traditionalManager: IConfigHeroTraditionalManager
) : IHeroDetails {
    override val upgradedHp: Int = if (obj.containsKey("upgraded_hp")) obj.getInt("upgraded_hp") else 0
    override val upgradedDmg: Int = if (obj.containsKey("upgraded_dmg")) obj.getInt("upgraded_dmg") else 0
    override val upgradedSpeed: Int = if (obj.containsKey("upgraded_speed")) obj.getInt("upgraded_speed") else 0
    override val upgradedRange: Int = if (obj.containsKey("upgraded_range")) obj.getInt("upgraded_range") else 0
    override val upgradedBomb: Int = if (obj.containsKey("upgraded_bomb")) obj.getInt("upgraded_bomb") else 0
    override val details: String = obj.getUtfString("gen_id")
    private val extractor = DetailsExtractor(details)
    override val color = extractor.extract(50, 5)
    override val skin = obj.getInt("skin").toInt()
    override val heroId = obj.getLong("bomber_id").toInt()
    override val index = extractor.extract(30, 10)
    override val rarity = extractor.extract(40, 5)
    override val level = extractor.extract(45, 5)
    override val stamina = extractor.extract(60, 5)
    override val bombSkin = extractor.extract(70, 5)
    override val bombPower = extractor.extract(80, 5)
    override val abilityList = HeroAbilityList(extractor.extractArray(90, 5, 5))
    override val abilityHeroSList = HeroAbilityList(extractor.extractArray(180, 5, 5))
    override val resetShieldCounter = extractor.extract(240, 5)
    override val shieldLevel = extractor.extract(235, 5)
    override val type: EnumConstants.HeroType = EnumConstants.HeroType.valueOf(obj.getInt("type"))
    override val heroConfig = traditionalManager.getConfigHero(skin, color)
    override val maxSpeed = heroConfig.maxSpeed + upgradedSpeed
    override val maxRange = heroConfig.maxRange + upgradedRange
    override val maxBomb = heroConfig.maxBomb + upgradedBomb
    override val dataType = dataType
    override val speed = heroConfig.speed
    override val bombCount = heroConfig.bomb
    override val bombRange = heroConfig.range
    override val hp = heroConfig.hp + upgradedHp
    override val dmg = heroConfig.dmg + upgradedDmg
    override val maxHp = heroConfig.maxHp
    override val maxDmg = heroConfig.maxDmg
    override val maxUpgradeSpeed = heroConfig.maxUpgradeSpeed
    override val maxUpgradeRange = heroConfig.maxUpgradeRange
    override val maxUpgradeBomb = heroConfig.maxUpgradeBomb
    override val maxUpgradeHp = heroConfig.maxUpgradeHp
    override val maxUpgradeDmg = heroConfig.maxUpgradeDmg
    override val heroTRType =
        if (!obj.containsKey("hero_tr_type")) HeroTRType.HERO else HeroTRType.valueOf(obj.getUtfString("hero_tr_type"))
    val status = obj.getInt("status").toInt()
    override fun isEqualTo(other: IHeroDetails): Boolean {
        return true
    }

    override fun isHeroS(): Boolean {
        return false
    }
}