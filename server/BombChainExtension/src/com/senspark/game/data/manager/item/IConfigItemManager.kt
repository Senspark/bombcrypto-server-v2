package com.senspark.game.data.manager.item

import com.senspark.common.service.IServerService
import com.senspark.game.constant.ItemType
import com.senspark.game.data.model.config.Item
import com.smartfoxserver.v2.entities.data.ISFSArray

interface IConfigItemManager : IServerService {
    val itemsDefault: List<Item>
    fun getItem(itemId: Int): Item
    fun toSFSArray(): ISFSArray
    fun get(itemType: ItemType): List<Item>
    fun getRandom(itemType: ItemType): Item?
    fun getItems(itemType: ItemType?): List<Item>

    /**
     * return default item (item free for all user)
     */
    fun getDefaultItems(itemType: ItemType): List<Item>
}