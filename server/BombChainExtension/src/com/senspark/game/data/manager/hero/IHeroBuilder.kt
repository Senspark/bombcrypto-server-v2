package com.senspark.game.data.manager.hero

import com.senspark.common.service.IServerService
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.nft.IHeroDetails
import com.senspark.game.declare.EnumConstants
import com.smartfoxserver.v2.entities.data.ISFSObject

interface IHeroBuilder : IServerService {
    fun getFiHeroes(uid: Int, dataType: EnumConstants.DataType): Map<Int, Hero>
    fun getFiHeroes(
        uid: Int,
        dataTypes: List<EnumConstants.DataType>,
        lstBbmId: List<Int>,
        type: EnumConstants.HeroType,
        listItemIds: List<Int>
    ): List<Hero>

    fun getTonHeroes(uid: Int, limit: Int = 0): Map<Int, Hero>
    fun getHeroesOldSeason(uid: Int, type: EnumConstants.HeroType): List<Hero>
    fun getHeroTraditional(
        uid: Int,
        configHeroTraditionalManager: IConfigHeroTraditionalManager
    ): MutableMap<Int, Hero>

    fun getHeroFiFromDatabase(dataType: EnumConstants.DataType, bomberId: List<Int>, type: Int): List<Hero>
    fun newInstance(userId: Int, details: IHeroDetails): Hero
    fun createHero(obj: ISFSObject, details: IHeroDetails): Hero
}