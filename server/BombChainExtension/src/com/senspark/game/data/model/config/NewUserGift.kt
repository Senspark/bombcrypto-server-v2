package com.senspark.game.data.model.config

import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.schema.TableNewUserGift
import org.jetbrains.exposed.sql.ResultRow

class NewUserGift(
    val item: Item,
    val quantity: Int,
    val expirationAfter: Long,
    val step: Int,
) {
    companion object {
        fun fromResultRow(resultRow: ResultRow, configItemManager: IConfigItemManager): NewUserGift {
            return NewUserGift(
                configItemManager.getItem(resultRow[TableNewUserGift.itemId]),
                resultRow[TableNewUserGift.quantity],
                resultRow[TableNewUserGift.expirationAfter] ?: 0,
                resultRow[TableNewUserGift.step],
            )
        }
    }
}