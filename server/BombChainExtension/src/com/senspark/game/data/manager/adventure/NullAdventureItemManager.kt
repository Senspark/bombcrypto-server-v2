package com.senspark.game.data.manager.adventure

import com.senspark.common.constant.PvPItemType
import com.senspark.game.data.model.config.AdventureItem
import com.senspark.game.exception.CustomException

class NullAdventureItemManager : IAdventureItemManager {

    override fun initialize() {
    }

    override fun get(itemType: PvPItemType): AdventureItem? {
        return null
    }

    override fun getItemRewardValue(itemType: PvPItemType): Int {
        return 0
    }

    override fun getRandomItem(itemContainedChest: Boolean): AdventureItem {
        throw CustomException("Feature not support")
    }
}