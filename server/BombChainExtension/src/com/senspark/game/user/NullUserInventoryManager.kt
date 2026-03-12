package com.senspark.game.user

import com.senspark.common.constant.ExpirationAfter
import com.senspark.common.constant.ItemId
import com.senspark.game.constant.ItemPackage
import com.senspark.game.constant.ItemType
import com.senspark.game.data.model.user.UserItem
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray

class NullUserInventoryManager : IUserInventoryManager {
    override fun loadInventory() {}

    override fun isHavingSkin(skinIds: List<Int>): Boolean {
        return true
    }

    override fun isHavingSkin(skinId: Int): Boolean {
        return true
    }

    override val activeSkinChests: Map<ItemType, List<ItemId>> = emptyMap()

    override fun activeSkinChest(itemId: Int, expirationAfter: Long?) {}

    override fun activeSkinChest(itemType: ItemType, items: Map<ItemId, ExpirationAfter?>) {}

    override fun openSkinChest(): SkinChest {
        throw CustomException("Feature not support")
    }

    override fun getInventoryToSFSArray(): ISFSArray {
        return SFSArray()
    }

    override fun buyCostumeItem(itemId: Int, itemPackage: ItemPackage, quantity: Int) {}
    override fun addItem(itemId: Int, quantity: Int, expiration: Long, reason: String?) {}

    override fun destroy() = Unit
}