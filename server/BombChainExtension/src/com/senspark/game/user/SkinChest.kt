package com.senspark.game.user

import com.senspark.game.constant.ItemType
import com.senspark.game.data.model.user.UserItem
import com.senspark.game.utils.sfsObjectOf
import com.smartfoxserver.v2.entities.data.ISFSObject
import java.time.Instant

class SkinChest(
    val id: Int,
    val itemId: Int,
    val type: ItemType,
    private var _status: Int,
    var expiryDate: Instant?,
    var expirationAfter: Long?,
) {
    val active get() = _status == 1

    constructor(data: UserItem) : this(
        data.id,
        data.itemId,
        data.type,
        data.status,
        if (data.expiryDate != null) Instant.ofEpochMilli(data.expiryDate) else null,
        data.expirationAfter
    )

    fun active() {
        _status = 1
    }

    fun deactive() {
        _status = 0
    }

    fun toSFSObject(): ISFSObject {
        return sfsObjectOf {
            putInt("id", id)
            putInt("item_id", itemId)
            putInt("type", type.value)
            putBool("active", active)
            if (expiryDate != null) putLong("expiry_date", expiryDate!!.toEpochMilli()) else putNull("expiry_date")
        }
    }
}