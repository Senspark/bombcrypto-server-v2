package com.senspark.game.data.manager.adventure

import com.senspark.common.constant.PvPItemType
import com.senspark.common.service.IServerService
import com.senspark.game.data.model.config.AdventureItem

interface IAdventureItemManager : IServerService {
    fun get(itemType: PvPItemType): AdventureItem?
    fun getItemRewardValue(itemType: PvPItemType): Int
    fun getRandomItem(itemContainedChest: Boolean): AdventureItem
}