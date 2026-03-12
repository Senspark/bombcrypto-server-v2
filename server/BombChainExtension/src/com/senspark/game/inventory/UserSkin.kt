package com.senspark.game.inventory

import com.senspark.common.constant.ItemId
import com.senspark.game.constant.ItemStatus
import com.senspark.game.data.model.Filter
import com.senspark.game.data.model.user.UserItem
import com.senspark.game.db.IUserDataAccess

class UserSkin(
    private val _userDataAccess: IUserDataAccess
): IUserInventory {
    
    override fun get(
        uid: Int,
        filter: Filter
    ): Map<ItemId, List<UserItem>> {
        return _userDataAccess.getUserInventory(
            uid,
            filter,
            ItemStatus.LockedOrEquipSkin.value
        )
    }

    override fun insert() {
        TODO("Not yet implemented")
    }

    override fun update() {
        TODO("Not yet implemented")
    }
}