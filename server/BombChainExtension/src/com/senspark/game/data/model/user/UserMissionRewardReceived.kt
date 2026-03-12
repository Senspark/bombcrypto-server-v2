package com.senspark.game.data.model.user

import com.senspark.game.data.model.config.Item
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserMissionRewardReceived(
    @SerialName("item_id")
    val itemId: Int,
    @SerialName("item_type")
    val itemType: String,
    val quantity: Int
) {
    constructor(item: Item, quantity: Int) : this(item.id, item.type.name, quantity)

    fun toSfsObject(): ISFSObject {
        return SFSObject().apply {
            putInt("item_id", itemId)
            putUtfString("item_type", itemType)
            putInt("quantity", quantity)
        }
    }
}