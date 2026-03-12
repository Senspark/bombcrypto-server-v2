package com.senspark.game.data.manager.grindHero

import com.senspark.common.utils.toSFSArray
import com.senspark.game.constant.ItemKind
import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.data.model.config.GrindHero
import com.senspark.game.data.model.config.HeroGrindDropItem
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.exception.CustomException
import com.senspark.game.utils.random.IWeightedRandom
import com.senspark.game.utils.random.WeightedRandomFloat
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class GrindHeroManager(
    private val _shopDataAccess: IShopDataAccess,
    private val _configItemManager: IConfigItemManager,
) : IGrindHeroManager {

    val items: MutableMap<ItemKind, GrindHero> = mutableMapOf()
    private val _randomManager: MutableMap<ItemKind, IWeightedRandom<HeroGrindDropItem>> = mutableMapOf()

    override fun initialize() {
        items.putAll(_shopDataAccess.loadGrindHeroConfig(_configItemManager))
        _randomManager.putAll(items.mapValues { item ->
            WeightedRandomFloat(item.value.dropItems)
        })
    }

    override fun toSfsArray(): ISFSArray {
        return items.toSFSArray {
            SFSObject().apply {
                putUtfString("item_kind", it.key.name)
                putInt("price", it.value.price)
                putUtfString("reward_type", BLOCK_REWARD_TYPE.GOLD.name)
            }
        }
    }

    override fun grind(itemKind: ItemKind, quantity: Int): Pair<GrindHero, MutableList<HeroGrindDropItem>> {
        val item = items[itemKind] ?: throw CustomException("Grind config not found kind ${itemKind.name}")
        val dropItems = mutableListOf<HeroGrindDropItem>()
        repeat(quantity) {
            val dropItem = _randomManager[itemKind]?.randomItem()
                ?: throw CustomException("Cannot random item kind ${itemKind.name}")
            dropItems.add(dropItem)
        }
        return Pair(item, dropItems)
    }
}