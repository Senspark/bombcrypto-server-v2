package com.senspark.game.data.manager.item

import com.senspark.game.constant.ItemType
import com.senspark.game.data.model.config.Item
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray

class NullConfigItemManager : IConfigItemManager {
    override val itemsDefault get() = emptyList<Item>()

    override fun initialize() {
    }

    override fun getItem(itemId: Int): Item {
        throw CustomException("Feature not support")
    }

    override fun get(itemType: ItemType): List<Item> {
        return emptyList()
    }

    override fun getRandom(itemType: ItemType): Item? {
        return null
    }

    override fun getItems(itemType: ItemType?): List<Item> {
        return emptyList()
    }

    override fun getDefaultItems(itemType: ItemType): List<Item> {
        return emptyList()
    }

    override fun toSFSArray(): ISFSArray {
        return SFSArray()
    }
}