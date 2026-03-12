package com.senspark.game.data.model.config

import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.Serializable

@Serializable
class MarketItemConfig(
    val itemId: Int,
    val minPrice: Double,
    private val isNoExpiration: Boolean,

    ) {
    fun toSfsObject(): ISFSObject {
        return SFSObject().apply {
            putInt("item_id", itemId)
            putDouble("min_price", minPrice)
            putBool("is_no_expired_item", isNoExpiration)
        }
    }
}