package com.senspark.game.data.model.config

import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.utils.deserializeList
import java.sql.ResultSet

class MarketplaceItem(
    val id: Int,
    val type: Int,
    val itemId: Int,
    val listId: List<Int>,
    val price: Float,
    val quantity: Int,
    val rewardType: Int,
    var uidCreator: Int,
    val name: String,
    val dateTime: Long
) {
    companion object {
        fun fromResultSet(rs: ResultSet, configProductManager: IConfigItemManager): MarketplaceItem {
            val itemId = rs.getInt("item_id")
            return MarketplaceItem(
                rs.getInt("id"),
                rs.getInt("type"),
                itemId,
                deserializeList<Int>(rs.getString("list_id")),
                rs.getDouble("price").toFloat(),
                rs.getInt("quantity"),
                rs.getInt("reward_type"),
                rs.getInt("uid_creator"),
                configProductManager.getItem(itemId).name,
                rs.getTimestamp("modify_date").time
            )
        }
    }
}