package com.senspark.game.data.manager.hero

import com.senspark.common.utils.toSFSArray
import com.senspark.game.data.model.nft.ConfigHeroTraditional
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.nft.IHeroDetails
import com.senspark.game.data.model.nft.NonFiHeroDetails
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.declare.EnumConstants
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject

class ConfigHeroTraditionalManager(
    private val _shopDataAccess: IShopDataAccess,
    private val _heroBuilder: IHeroBuilder,
) : IConfigHeroTraditionalManager {

    private val _data: MutableMap<Int, ConfigHeroTraditional> = mutableMapOf()
    override val itemIds: MutableList<Int> = mutableListOf()
    override val skinDefault: MutableList<Int> = mutableListOf()

    override fun initialize() {
        _data.putAll(_shopDataAccess.getHeroTraditionalConfigs())
        itemIds.addAll(_data.keys.toList())
        skinDefault.addAll(_data.values.filter { it.tutorial == 1 }.map { it.itemId })
    }

    override fun toSFSArray(): ISFSArray {
        return _data.toSFSArray { it.value.toSFSObject() }
    }

    override fun getConfigHero(skin: Int, color: Int): ConfigHeroTraditional {
        return _data.filter { it.value.skin == skin && it.value.color == color }.values.first()
    }

    override fun getItemId(skin: Int, color: Int): Int {
        return _data.filter { it.value.skin == skin && it.value.color == color }.keys.first()
    }

    override fun createHero(id: Int): IHeroDetails {
        return NonFiHeroDetails(
            _data[id]?.toSFSObject() ?: throw Exception("id $id not exists in data"),
            EnumConstants.DataType.TR,
            this
        )
    }

    override fun createHero(itemId: Int, userId: Int): Hero {
        val hero = createHero(itemId)
        return _heroBuilder.newInstance(userId, hero)
    }

    override fun createHero(obj: ISFSObject): Hero {
        require(obj.getInt("type") == EnumConstants.HeroType.TR.value) {
            "Hero type must be TR"
        }
        val dataType = EnumConstants.DataType.valueOf(obj.getUtfString("data_type"))
        val details = NonFiHeroDetails(obj, dataType, this)
        return _heroBuilder.createHero(obj, details)
    }

    override fun getAllConfigs(): List<ConfigHeroTraditional> {
        return _data.values.toList()
    }
}