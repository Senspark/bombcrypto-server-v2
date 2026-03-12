package com.senspark.game.data.model.config

import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.schema.TableMysteryBox
import org.jetbrains.exposed.sql.ResultRow

class MysteryBox(
    override val item: Item,
    override val weight: Float,
    override val quantity: Int,
    override val expirationAfter: Long?
) : IMysteryBox {
    companion object {
        fun fromResultRow(resultRow: ResultRow, configItemManager: IConfigItemManager): MysteryBox {
            return MysteryBox(
                configItemManager.getItem(resultRow[TableMysteryBox.itemId]),
                resultRow[TableMysteryBox.weight].toFloat(),
                resultRow[TableMysteryBox.quantity],
                resultRow[TableMysteryBox.expirationAfter],
            )
        }
    }
}