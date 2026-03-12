@file:OptIn(ExperimentalSerializationApi::class)

package com.senspark.game.data.model.user

import com.senspark.game.constant.ItemType
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode.ALWAYS
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class UserItem(
    val id: Int,
    val type: ItemType,
    @SerialName("item_id")
    val itemId: Int,
    var status: Int,
    @SerialName("equip_status")
    var equipStatus: Int,
    @SerialName("create_date")
    val createDate: Long,
    @SerialName("expiry_date")
    val expiryDate: Long?,
    @EncodeDefault(ALWAYS)
    var quantity: Int = 1,
    val expirationAfter: Long?,
    val itemInstantIds: List<Int> = emptyList()
)