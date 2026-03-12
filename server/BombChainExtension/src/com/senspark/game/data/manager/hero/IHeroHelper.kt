package com.senspark.game.data.manager.hero

import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.nft.IHeroShieldBuilder

interface IHeroHelper {
    val heroShieldBuilder: IHeroShieldBuilder
    fun getDamageTreasure(hero: Hero): Int
    fun getDamageJail(hero: Hero): Int
    fun getTotalPower(hero: Hero): Int
    fun isFakeS(hero: Hero): Boolean
    fun getPercentSaveEnergy(hero: Hero): Int
}