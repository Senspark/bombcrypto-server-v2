package com.senspark.game.data.model.nft

import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.EnumConstants.HeroType
import com.senspark.game.utils.DetailsExtractor

class BlockchainHeroDetails(override val details: String, override val dataType: EnumConstants.DataType) : IHeroDetails {
    private val extractor = DetailsExtractor(details)
    override val heroId = extractor.extract(0, 30)
    override val index = extractor.extract(30, 10)
    override val rarity = extractor.extract(40, 5)
    override val level = extractor.extract(45, 5)
    override val color = extractor.extract(50, 5)
    override val skin = extractor.extract(55, 5)
    override val stamina = extractor.extract(60, 5)
    override val speed = extractor.extract(65, 5)
    override val bombSkin = extractor.extract(70, 5)
    override val bombCount = extractor.extract(75, 5)
    override val bombPower = extractor.extract(80, 5)
    override val bombRange = extractor.extract(85, 5)
    override val abilityList = HeroAbilityList(extractor.extractArray(90, 5, 5))
    override val abilityHeroSList = HeroAbilityList(extractor.extractArray(180, 5, 5))
    override val resetShieldCounter = extractor.extract(240, 5)
    override val shieldLevel = extractor.extract(235, 5)
    override val type = HeroType.FI
    override val maxSpeed = speed
    override val maxRange = bombRange
    override val maxBomb = bombCount
    override val hp = 0
    override val dmg = 0
    override val maxHp = 0
    override val maxDmg = 0
    override val maxUpgradeSpeed = 0
    override val maxUpgradeRange = 0
    override val maxUpgradeBomb = 0
    override val maxUpgradeHp = 0
    override val maxUpgradeDmg = 0
    override val heroTRType = EnumConstants.HeroTRType.HERO
    override val heroConfig = null

    override fun isEqualTo(other: IHeroDetails): Boolean {
        return heroId == other.heroId && extractor.value shr 40 == (other as BlockchainHeroDetails).extractor.value shr 40
    }

    override fun isHeroS(): Boolean {
        return abilityHeroSList.items.isNotEmpty()
    }

    override val upgradedSpeed = 0
    override val upgradedRange = 0
    override val upgradedBomb = 0
    override val upgradedHp = 0
    override val upgradedDmg = 0
}