package com.senspark.game.data.model.user

import com.senspark.common.constant.ItemId
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
class UserMailItem(
    @SerialName("item_id")
    val itemId: ItemId,
    val quantity: Int,
    @SerialName("is_lock")
    val isLock: Boolean,
    @SerialName("expiration_after")
    val expirationAfter: Long
) {
    fun toSfsObject(): ISFSObject {
        return SFSObject().apply {
            putInt("item_id", itemId)
            putInt("quantity", quantity)
            putBool("is_lock", isLock)
            putLong("expiration_after", expirationAfter)
        }
    }
}