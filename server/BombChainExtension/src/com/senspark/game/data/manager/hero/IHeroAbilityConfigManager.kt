package com.senspark.game.data.manager.hero

import com.senspark.common.service.IGlobalService
import com.senspark.game.data.model.config.HeroAbilityConfig

interface IHeroAbilityConfigManager : IGlobalService {
    fun getConfig(ability: Int): HeroAbilityConfig
}