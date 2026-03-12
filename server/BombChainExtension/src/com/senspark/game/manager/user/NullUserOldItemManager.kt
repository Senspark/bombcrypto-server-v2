package com.senspark.game.manager.user

import com.senspark.common.constant.ItemId

/**
 * Có lẽ mục đích là để hiển thị text "NEW" trên item mới
 */
class NullUserOldItemManager : IUserOldItemManager {

    override fun isNewItem(itemId: ItemId): Boolean {
        return false
    }

    override fun checkAndAddOldItem(itemId: ItemId) {}
}