package com.senspark.game.data.manager.hero

import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.nft.IHeroShieldBuilder

interface IHeroHelper {
    val heroShieldBuilder: IHeroShieldBuilder
    fun getDamageTreasure(hero: Hero): Int
    fun getDamageJail(hero: Hero): Int
    fun getTotalPower(hero: Hero): Int

    /// Stamina base + o ganho dos niveis 6-10. Recebe primitivos porque e chamado tanto com um
    /// [Hero] montado quanto com um IHeroDetails cru, antes do Hero existir.
    fun getTotalStamina(rarity: Int, level: Int, baseStamina: Int): Int

    /// Energia maxima do hero. Unico lugar que conhece a conversao stamina -> energia.
    fun getMaxEnergy(rarity: Int, level: Int, baseStamina: Int): Int
    fun isFakeS(hero: Hero): Boolean
    fun getPercentSaveEnergy(hero: Hero): Int
}