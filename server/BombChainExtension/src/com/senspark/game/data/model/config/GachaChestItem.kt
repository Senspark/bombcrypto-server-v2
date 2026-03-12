package com.senspark.game.data.model.config

import com.senspark.game.declare.customEnum.GachaChestType
import com.senspark.lib.utils.Util
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


interface IGachaChestItem : IHasWeightEntity {
    val itemId: Int
    val chestType: GachaChestType
    val quantity: Int
    val item: Item
    val expirationAfter: Long
    val isLock: Boolean
    fun toSfsObject(): ISFSObject
}

@Serializable
class GachaChestItem(
    @SerialName("item_id")
    override val itemId: Int,
    @SerialName("chest_type")
    private val _chestType: Int,
    private val min: Int,
    private val max: Int,
    @SerialName("drop_rate")
    override val weight: Float,
    @SerialName("expiration_after")
    private val _expirationAfter: Long?,
    @SerialName("is_lock")
    override val isLock: Boolean
) : IGachaChestItem {

    @Transient
    override lateinit var item: Item

    @Transient
    override val chestType: GachaChestType = GachaChestType.fromValue(_chestType)

    override val expirationAfter = _expirationAfter ?: 0L

    override val quantity: Int
        get() {
            return if (min == max) {
                min
            } else {
                Util.randInt(min, max)
            }
        }

    override fun toSfsObject(): ISFSObject {
        return SFSObject().apply {
            putInt("item_id", itemId)
            putInt("quantity", quantity)
        }
    }
}

class GachaChestItemRandomized(
    override val itemId: Int,
    override val chestType: GachaChestType,
    override val quantity: Int,
    override val item: Item,
    override val weight: Float,
    override val expirationAfter: Long,
    override val isLock: Boolean
) : IGachaChestItem {

    constructor(item: IGachaChestItem) : this(
        item.itemId,
        item.chestType,
        item.quantity,
        item.item,
        item.weight,
        item.expirationAfter,
        item.isLock
    )

    override fun toSfsObject(): ISFSObject {
        return SFSObject().apply {
            putInt("item_id", itemId)
            putInt("quantity", quantity)
        }
    }
} 