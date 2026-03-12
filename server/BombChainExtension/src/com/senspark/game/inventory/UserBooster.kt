package com.senspark.game.inventory

import com.senspark.game.data.model.Filter
import com.senspark.game.data.model.user.UserItem
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.db.IUserDataAccess
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UserBooster(
    private val _userDataAccess: IUserDataAccess
) : IUserInventory {

    override fun get(
        uid: Int, filter: Filter
    ): Map<Int, List<UserItem>> {
        return _userDataAccess.getUserBooster(uid, filter)
    }

    override fun insert() {
        TODO("Not yet implemented")
    }

    override fun update() {
        TODO("Not yet implemented")
    }
}