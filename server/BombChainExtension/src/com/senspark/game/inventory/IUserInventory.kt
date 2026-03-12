package com.senspark.game.inventory

import com.senspark.game.data.model.Filter
import com.senspark.game.data.model.user.UserItem

interface IUserInventory {
    fun get(
        uid: Int,
        filter: Filter
    ): Map<Int, List<UserItem>>

    fun insert()
    fun update()
}