package com.senspark.game.manager.user

import com.senspark.common.constant.ItemId
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.db.IUserDataAccess

/**
 * Có lẽ mục đích là để hiển thị text "NEW" trên item mới
 */
class UserOldItemManager(
    private val _mediator: UserControllerMediator,
) : IUserOldItemManager {

    private val configItemManager = _mediator.svServices.get<IConfigItemManager>()
    private val userDataAccess = _mediator.services.get<IUserDataAccess>()

    private var oldItemIds: MutableSet<ItemId> = userDataAccess.loadUserOldItem(_mediator.userId).toMutableSet()

    override fun isNewItem(itemId: ItemId): Boolean {
        return !oldItemIds.contains(itemId)
    }

    override fun checkAndAddOldItem(itemId: ItemId) {
        val item = configItemManager.getItem(itemId)
        if (isNewItem(item.id)) {
            oldItemIds.add(item.id)
            userDataAccess.saveUserOldItem(_mediator.userId, oldItemIds)
        }
    }


}