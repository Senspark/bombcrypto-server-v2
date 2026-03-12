package com.senspark.game.data.manager.hero

import com.senspark.common.service.IServerService
import com.senspark.game.data.model.nft.ConfigHeroTraditional
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.nft.IHeroDetails
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject

interface IConfigHeroTraditionalManager : IServerService {

    val itemIds: List<Int>
    val skinDefault: List<Int>

    fun toSFSArray(): ISFSArray
    fun getConfigHero(skin: Int, color: Int): ConfigHeroTraditional
    fun getItemId(skin: Int, color: Int): Int
    fun createHero(id: Int): IHeroDetails
    fun getAllConfigs(): List<ConfigHeroTraditional>
    fun createHero(itemId: Int, userId: Int): Hero
    fun createHero(obj: ISFSObject): Hero
} 