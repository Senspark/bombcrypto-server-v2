package com.senspark.game.data.model.config

import com.senspark.common.constant.ItemId
import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.data.model.user.AddUserItemWrapper
import com.senspark.game.declare.customEnum.SubscriptionProduct
import com.senspark.game.utils.deserializeList
import com.senspark.lib.utils.Util
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.Serializable
import java.sql.ResultSet

class SubscriptionPackage(
    val id: SubscriptionProduct,
    val no: Int,
    val name: String,
    val description: String,
    val noAds: Boolean,
    val offlineRewardBonus: Int,
    val pvpPenalty: Boolean,
    val bonusPvpPointRate: Float,
    val adventureBonusItemRate: Float,
    val gemPackageBonus: Float,
    randomItemJson: String,
) {
    private val randomItems = deserializeList<SubscriptionRandomItem>(randomItemJson)

    companion object {
        fun fromResultSet(rs: ResultSet): SubscriptionPackage {
            return SubscriptionPackage(
                SubscriptionProduct.valueOf(rs.getString("id")),
                rs.getInt("no"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getBoolean("no_ads"),
                rs.getInt("offline_reward_bonus"),
                rs.getBoolean("pvp_penalty"),
                rs.getFloat("bonus_pvp_point_rate"),
                rs.getFloat("adventure_bonus_item_rate"),
                rs.getFloat("gem_package_bonus"),
                rs.getString("random_items")
            )
        }
    }

    fun toSfsObject(): ISFSObject {
        return SFSObject().apply {
            putUtfString("product_id", id.normalizeName)
            putUtfString("description", description)
            putUtfString("name", name)
        }
    }

    fun getRewards(itemConfigItemManager: IConfigItemManager): List<AddUserItemWrapper> {
        val rewards = mutableListOf<AddUserItemWrapper>()
        randomItems.forEach {
            repeat(it.quantity) { _ ->
                val itemId = it.itemIds[Util.randIndex(it.itemIds.size)]
                rewards.add(
                    AddUserItemWrapper(
                        itemConfigItemManager.getItem(itemId),
                        1
                    )
                )
            }
        }
        return rewards.groupBy { it.item }.map { AddUserItemWrapper(it.key, it.value.sumOf { it2 -> it2.quantity }) }
    }
}


@Serializable
class SubscriptionRandomItem(
    val quantity: Int,
    val itemIds: List<ItemId>
)