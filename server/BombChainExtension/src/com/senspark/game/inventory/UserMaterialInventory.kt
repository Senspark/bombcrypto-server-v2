package com.senspark.game.inventory

import com.senspark.game.constant.ItemType
import com.senspark.game.data.model.Filter
import com.senspark.game.data.model.user.UserItem
import com.senspark.game.manager.material.IUserMaterialManager
import java.time.Instant

class UserMaterialInventory(
    private val userMaterialManager: IUserMaterialManager
) : IUserInventory {

    override fun get(uid: Int, filter: Filter): Map<Int, List<UserItem>> {
        return userMaterialManager.getMaterials().mapValues {
            listOf(
                UserItem(
                    id = it.key,
                    type = ItemType.MATERIAL,
                    itemId = it.key,
                    status = 1,
                    equipStatus = 1,
                    Instant.now().toEpochMilli(),
                    null,
                    quantity = it.value.quantity,
                    expirationAfter = null
                )
            )
        }
    }

    override fun insert() {
        TODO("Not yet implemented")
    }

    override fun update() {
        TODO("Not yet implemented")
    }
}