package com.senspark.game.user

import com.senspark.common.constant.ExpirationAfter
import com.senspark.common.constant.ItemId
import com.senspark.common.service.IService
import com.senspark.common.service.Service
import com.senspark.game.constant.ItemPackage
import com.senspark.game.constant.ItemType
import com.smartfoxserver.v2.entities.data.ISFSArray

@Service("IUserInventoryManager")
interface IUserInventoryManager : IService {
    /**
     * map of <itemType, List<itemId>
     */
    val activeSkinChests: Map<ItemType, List<ItemId>>
    fun loadInventory()

    /**
     * active skin
     * @param itemId id of item active
     * @param expirationAfter thời hạn của item
     */
    fun activeSkinChest(itemId: Int, expirationAfter: Long?)

    /**
     * active skin
     * @param itemType
     */
    fun activeSkinChest(itemType: ItemType, items: Map<ItemId, ExpirationAfter?>)

    @Throws(Exception::class)
    fun openSkinChest(): SkinChest
    fun getInventoryToSFSArray(): ISFSArray

    companion object {
        const val DEFAULT_SKIN_ITEM_EXPIRY_TIME_IN_MILLIS: Long = 7 * 24 * 60 * 60 * 1000
    }

    fun buyCostumeItem(itemId: Int, itemPackage: ItemPackage, quantity: Int)
    fun addItem(itemId: Int, quantity: Int, expiration: Long, reason: String? = null)
    fun isHavingSkin(skinIds: List<Int>): Boolean
    fun isHavingSkin(skinId: Int): Boolean
}