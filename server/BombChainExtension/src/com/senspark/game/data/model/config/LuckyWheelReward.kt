package com.senspark.game.data.model.config

import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.schema.TableConfigLuckyWheelReward
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import org.jetbrains.exposed.sql.ResultRow

class LuckyWheelReward(
    val code: String,
    val item: Item?,
    val quantity: Int,
    override val weight: Float
) : IHasWeightEntity {

    val itemType = item?.type

    fun toSfsObject(): ISFSObject {
        return SFSObject().apply {
            putUtfString("reward_code", code)
            putUtfString("item_type", item?.type?.name ?: "")
            putInt("item_id", item?.id ?: -1)
            putInt("quantity", quantity)
        }
    }

    companion object {
        fun fromResultRow(result: ResultRow, configItemManager: IConfigItemManager): LuckyWheelReward {
            val itemId = result[TableConfigLuckyWheelReward.itemId]
            val item = itemId?.let { configItemManager.getItem(itemId) }
            return LuckyWheelReward(
                result[TableConfigLuckyWheelReward.code],
                item,
                result[TableConfigLuckyWheelReward.quantity] ?: 0,
                result[TableConfigLuckyWheelReward.weight].toFloat()
            )
        }
    }
}