package com.senspark.game.data.model.config

import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.declare.EnumConstants
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.Serializable
import java.sql.ResultSet
class MarketplaceItemGrouped(
    val item: Item,
    val price: Float,
    val quantity: Int,
    val rewardType: Int,
    val expirationAfter: Int
) {
    companion object {
        fun fromResultSet(rs: ResultSet, configProductManager: IConfigItemManager): MarketplaceItemGrouped {
            return MarketplaceItemGrouped(
                configProductManager.getItem(rs.getInt("item_id")),
                rs.getDouble("price").toFloat(),
                rs.getInt("quantity"),
                EnumConstants.BLOCK_REWARD_TYPE.valueOf(rs.getString("reward_type")).value ,
                rs.getInt("expiration_after")
            )
        }
        fun fromMyItemList(myItems: List<MyItemMarket>, configItemManager: IConfigItemManager): List<MarketplaceItemGrouped> {
            return myItems.map { myItem ->
                MarketplaceItemGrouped(
                    configItemManager.getItem(myItem.itemId),
                    myItem.price,
                    myItem.quantity,
                    EnumConstants.BLOCK_REWARD_TYPE.valueOf(myItem.rewardType).value,
                    myItem.expirationAfter
                )
            }
        }
    }
    

    fun toSfsObject(): ISFSObject {
        return SFSObject().apply {
            putInt("item_id", item.id)
            putFloat("price", price)
            putInt("quantity", quantity)
            putInt("reward_type", rewardType)
            putInt("id", 0)
            putInt("expiration_after", expirationAfter)
        }
    }
}

@Serializable
data class MyItemMarket(
    val itemId: Int,
    val price: Float,
    val quantity: Int,
    val rewardType: String,
    val expirationAfter: Int,
)