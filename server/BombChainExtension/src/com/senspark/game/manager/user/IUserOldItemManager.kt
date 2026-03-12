package com.senspark.game.manager.user

import com.senspark.common.constant.ItemId

interface IUserOldItemManager {
    fun isNewItem(itemId: ItemId): Boolean
    fun checkAndAddOldItem(itemId: ItemId)
} 