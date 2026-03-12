package com.senspark.game.data.model.config

import com.senspark.common.constant.ItemId
import com.senspark.common.utils.toSFSArray
import com.senspark.game.declare.customEnum.IAPShopBonusType
import com.senspark.game.declare.customEnum.IAPShopType
import com.senspark.game.declare.customTypeAlias.ProductId
import com.senspark.game.declare.customTypeAlias.Quantity
import com.senspark.game.utils.deserializeList
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.sql.ResultSet

class IAPShopConfig(
    val productId: ProductId,
    val type: IAPShopType,
    val name: String,
    val limitPerUser: Int?,
    itemJson: String,
    itemBonusJson: String,
    private val bonusType: IAPShopBonusType,
    val isStaterPack: Boolean,
    val isRemoveAds: Boolean,
    /**
     * thứ tự mua, xuất hiện của các gói
     */
    val buyStep: Int,
    /**
     * giới hạn mua theo giây
     */
    val purchaseTimeLimit: Long?,
) {
    var canBuySpecialOffer: Boolean = false
    val items = deserializeList<IAPShopConfigItem>(itemJson)
    val itemsBonus = deserializeList<IAPShopConfigItem>(itemBonusJson)

    companion object {
        @JvmStatic
        fun fromResultSet(rs: ResultSet): IAPShopConfig {
            return IAPShopConfig(
                rs.getString("product_id"),
                IAPShopType.valueOf(rs.getString("type")),
                rs.getString("name"),
                if (rs.getObject("limit_per_user") == null) null else rs.getInt("limit_per_user"),
                rs.getString("items"),
                rs.getString("items_bonus"),
                IAPShopBonusType.values()[rs.getInt("bonus_type")],
                rs.getBoolean("is_stater_pack"),
                rs.getBoolean("is_remove_ads"),
                rs.getInt("buy_step"),
                if (rs.getObject("purchase_time_limit") == null) null else rs.getLong("purchase_time_limit"),
            )
        }
    }

    fun canBuyMore(purchasedCount: Int) = purchasedCount < (limitPerUser ?: Int.MAX_VALUE)

    fun toSFSObject(purchasedPackages: Map<ProductId, Quantity> = emptyMap()): ISFSObject {
        val hasPurchased = purchasedPackages.containsKey(productId)
        return SFSObject.newInstance().apply {
            putUtfString("product_id", productId)
            putUtfString("name", name)
            putInt("bonus_type", bonusType.ordinal)
            putBool("can_buy_special_offer", canBuySpecialOffer)
            putBool("is_stater_pack", isStaterPack)
            putBool("is_remove_ads", isRemoveAds)
            if (items.isNotEmpty()) {
                putSFSArray("items", items.toSFSArray { it.toSfsObject() })
            }
            if (items.isNotEmpty()) {
                putSFSArray("items_bonus", itemsBonus.toSFSArray { it.toSfsObject(hasPurchased) })
            }
        }
    }

    fun getItemBonus(purchasedPackages: Map<ProductId, Quantity>): List<IAPShopConfigItem> {
        return when (bonusType) {
            IAPShopBonusType.NONE -> emptyList()
            IAPShopBonusType.FIRST_PURCHASE -> {
                if (purchasedPackages.contains(productId)) {
                    return emptyList()
                } else {
                    itemsBonus
                }
            }
        }
    }
}

@Serializable
class IAPShopConfigItem(
    @SerialName("item_id")
    val itemId: ItemId,
    val quantity: Int,
    @SerialName("expiration_after")
    val expirationAfter: Long = 0
) {
    fun toSfsObject(isResetQuantity: Boolean = false): ISFSObject {
        return SFSObject().apply {
            putInt("item_id", itemId)
            putInt("quantity", if (isResetQuantity) 0 else quantity)
            putLong("expiration_after", expirationAfter)
        }
    }
}