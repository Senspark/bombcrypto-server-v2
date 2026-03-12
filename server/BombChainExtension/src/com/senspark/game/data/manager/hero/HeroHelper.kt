package com.senspark.game.data.manager.hero

import com.senspark.game.data.model.nft.*
import com.senspark.game.declare.GameConstants.BOMBER_ABILITY
import com.senspark.game.manager.stake.IHeroStakeManager

class HeroHelper(
    private val _heroStakeManager: IHeroStakeManager,
    private val _heroAbilityConfigManager: IHeroAbilityConfigManager,
    private val _heroUpgradePowerManager: IHeroUpgradePowerManager,
    override val heroShieldBuilder: IHeroShieldBuilder,
) : IHeroHelper {
    override fun getDamageTreasure(hero: Hero): Int {
        if (hero.abilityList.has(BOMBER_ABILITY.TREASURE_HUNTER)) {
            return _heroAbilityConfigManager.getConfig(BOMBER_ABILITY.TREASURE_HUNTER).value.toInt()
        }
        return 0
    }

    override fun getDamageJail(hero: Hero): Int {
        if (hero.abilityList.has(BOMBER_ABILITY.JAIL_BREAKER)) {
            _heroAbilityConfigManager.getConfig(BOMBER_ABILITY.JAIL_BREAKER).value.toInt()
        }
        return 0
    }

    override fun getTotalPower(hero: Hero): Int {
        return hero.bombPower + if (hero.level == 1) {
            0
        } else _heroUpgradePowerManager.getPowerIncrease(hero.rarity, hero.level)
    }

    override fun isFakeS(hero: Hero): Boolean {
        return if (hero.stakeBcoin >= _heroStakeManager.minStakeHeroConfig[hero.rarity]!!) true else false
    }

    override fun getPercentSaveEnergy(hero: Hero): Int {
        if (hero.abilityList.has(BOMBER_ABILITY.SAVE_BATTERY)) {
            return _heroAbilityConfigManager.getConfig(BOMBER_ABILITY.SAVE_BATTERY).value.toInt()
        }
        return 0
    }
}