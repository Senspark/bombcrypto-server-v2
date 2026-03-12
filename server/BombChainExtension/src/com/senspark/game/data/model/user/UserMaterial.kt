package com.senspark.game.data.model.user

import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.data.model.config.Item
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import java.sql.ResultSet

class UserMaterial(
    val uid: Int,
    val item: Item,
    var quantity: Int,
) {
    companion object {
        fun fromResultSet(rs: ResultSet, configItemManager: IConfigItemManager): UserMaterial {
            return UserMaterial(
                rs.getInt("uid"),
                configItemManager.getItem(rs.getInt("item_id")),
                rs.getInt("quantity")
            )
        }
    }

    fun toSfsObject(): ISFSObject {
        return SFSObject().apply {
            putInt("item_id", item.id)
            putInt("quantity", quantity)
        }
    }
}