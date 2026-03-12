package com.senspark.game.data.manager.adventure

import com.senspark.common.constant.PvPItemType
import com.senspark.game.data.model.config.AdventureItem
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.declare.ErrorCode
import com.senspark.game.exception.CustomException
import com.senspark.game.utils.Utils

class AdventureItemManager(
    private val _shopDataAccess: IShopDataAccess,
) : IAdventureItemManager {

    private var _items: MutableMap<PvPItemType, AdventureItem> = mutableMapOf()
    private var _itemList: List<AdventureItem> = listOf()

    override fun initialize() {
        _items.putAll(_shopDataAccess.loadAdventureItem())
        _itemList = _items.values.toList()
    }

    override fun get(itemType: PvPItemType): AdventureItem? {
        return _items[itemType]
    }

    override fun getItemRewardValue(itemType: PvPItemType): Int {
        val item = _items[itemType] ?: throw CustomException(
            "Cannot find item with type ${itemType.name}",
            ErrorCode.SERVER_ERROR
        )
        return item.rewardValue
    }

    override fun getRandomItem(itemContainedChest: Boolean): AdventureItem {
        val filterItems =
            if (itemContainedChest) _itemList.filter { !it.type.isChest } else _itemList
        val totalDropRate = filterItems.sumOf { it.dropRate }
        val rand = Utils.randInt(1, totalDropRate)
        var dropPassed = 0
        filterItems.forEach {
            if (rand <= dropPassed + it.dropRate) {
                return it
            }
            dropPassed += it.dropRate
        }
        throw RuntimeException("Random Adventure Item failed")
    }
}