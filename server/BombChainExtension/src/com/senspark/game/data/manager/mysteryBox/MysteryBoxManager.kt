package com.senspark.game.data.manager.mysteryBox

import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.data.model.config.IMysteryBox
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.utils.random.IWeightedRandom
import com.senspark.game.utils.random.WeightedRandomFloat

class MysteryBoxManager(
    private val _configItemManager: IConfigItemManager,
    private val _shopDataAccess: IShopDataAccess,
) : IMysteryBoxManager {
    
    val items: MutableList<IMysteryBox> = mutableListOf()
    private lateinit var randomManger: IWeightedRandom<IMysteryBox>

    override fun initialize() {
        items.addAll(_shopDataAccess.loadMysteryBox(_configItemManager))
        randomManger = WeightedRandomFloat(items)
    }

    override fun getRandomItem(): IMysteryBox {
        return randomManger.randomItem()
    }
}