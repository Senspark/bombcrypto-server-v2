package com.senspark.game.data.model.config

import com.senspark.common.utils.toSFSArray
import com.senspark.game.declare.customEnum.ConfigUpgradeHeroType
import com.senspark.game.utils.deserializeList
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.sql.ResultSet

class UpgradeHeroTr(
    val index: Int,
    val type: ConfigUpgradeHeroType,
    val goldFee: Int,
    val gemFee: Int,
    private val itemsJson: String
) {

    val items = deserializeList<UpgradeHeroTrItem>(itemsJson)

    companion object {
        fun fromResultSet(rs: ResultSet): UpgradeHeroTr {
            return UpgradeHeroTr(
                rs.getInt("index"),
                ConfigUpgradeHeroType.valueOf(rs.getString("type")),
                rs.getInt("gold_fee"),
                rs.getInt("gem_fee"),
                rs.getString("items"),
            )
        }
    }

    fun toSfsObject(): ISFSObject {
        return SFSObject().apply {
            putInt("index", index)
            putInt("gold_fee", goldFee)
            putInt("gem_fee", gemFee)
            putSFSArray("items", items.toSFSArray { it.toSfsObject() })
        }
    }
}


@Serializable
class UpgradeHeroTrItem(
    @SerialName("item_id")
    val itemId: Int,
    val quantity: Int,
) {
    fun toSfsObject(): ISFSObject {
        return SFSObject().apply {
            putInt("item_id", itemId)
            putInt("quantity", quantity)
        }
    }
}