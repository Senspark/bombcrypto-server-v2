package com.senspark.game.data.manager.hero

import com.senspark.game.data.model.nft.ConfigHeroTraditional
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.nft.IHeroDetails
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray

class NullConfigHeroTraditionalManager : IConfigHeroTraditionalManager {

    override val itemIds get() = emptyList<Int>()
    override val skinDefault get() = emptyList<Int>()

    override fun initialize() {
    }
    
    override fun toSFSArray(): ISFSArray {
        return SFSArray()
    }

    override fun getConfigHero(skin: Int, color: Int): ConfigHeroTraditional {
        throw CustomException("Feature not support")
    }

    override fun getItemId(skin: Int, color: Int): Int {
        return 0
    }

    override fun createHero(id: Int): IHeroDetails {
        throw CustomException("Feature not support")
    }

    override fun createHero(itemId: Int, userId: Int): Hero {
        throw CustomException("Feature not support")
    }

    override fun createHero(obj: ISFSObject): Hero {
        throw CustomException("Feature not support")
    }

    override fun getAllConfigs(): List<ConfigHeroTraditional> {
        return emptyList()
    }
}