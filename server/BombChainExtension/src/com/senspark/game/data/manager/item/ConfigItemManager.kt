package com.senspark.game.data.manager.item

import com.senspark.common.utils.toSFSArray
import com.senspark.game.constant.ItemType
import com.senspark.game.data.model.config.Item
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.db.IShopDataAccess
import com.senspark.lib.utils.Util
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray

class ConfigItemManager(
    private val _shopDataAccess: IShopDataAccess,
) : IConfigItemManager {
    private val _data: MutableMap<Int, Item> = mutableMapOf()
    private val itemMapByItemType: MutableMap<ItemType, List<Item>> = mutableMapOf()
    private val itemDefaultByItemType: MutableMap<ItemType, List<Item>> = mutableMapOf()
    override val itemsDefault: MutableList<Item> = mutableListOf()

    override fun initialize() {
        _data.putAll(_shopDataAccess.getConfigItem())
        itemMapByItemType.putAll(_data.values.groupBy({ it.type }, { it }))
        itemDefaultByItemType.putAll(itemMapByItemType.mapValues { it.value.filter { it2 -> it2.isDefault } })
        itemsDefault.addAll(itemDefaultByItemType.map { it.value }.flatten())
    }

    override fun getItem(itemId: Int): Item {
        return _data[itemId] ?: throw Exception("Item $itemId not exists")
    }

    override fun get(itemType: ItemType): List<Item> {
        return itemMapByItemType[itemType] ?: emptyList()
    }

    override fun getRandom(itemType: ItemType): Item? {
        val items = get(itemType).filter { it.isSellable }
        return if (items.isEmpty()) {
            null
        } else {
            items[Util.randIndex(items.size)]
        }
    }

    override fun getItems(itemType: ItemType?): List<Item> {
        return itemMapByItemType[itemType] ?: emptyList()
    }

    override fun getDefaultItems(itemType: ItemType): List<Item> {
        return itemDefaultByItemType[itemType] ?: emptyList()
    }

    override fun toSFSArray(): ISFSArray {
        val results = SFSArray()
        if (_data.isEmpty()) return results
        return _data.values.toSFSArray { it.toSFSObject() }
    }
}