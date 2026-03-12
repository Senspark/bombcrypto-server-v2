package com.senspark.game.data.model.config

import com.senspark.game.constant.ItemKind
import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.schema.TableGrindHero
import com.senspark.game.utils.deserializeList
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow

class GrindHero(
    val itemKind: ItemKind,
    val price: Int,
    val dropItems: List<HeroGrindDropItem>
) {
    @Serializable
    class HeroGrindDropItemWrapper(
        val itemId: Int,
        val quantity: Int,
        val weight: Int
    )

    companion object {
        fun fromResultRow(resultRow: ResultRow, configItemManager: IConfigItemManager): GrindHero {
            val dropItemsWrapper = deserializeList<HeroGrindDropItemWrapper>(resultRow[TableGrindHero.dropItems])
            return GrindHero(
                ItemKind.valueOf(resultRow[TableGrindHero.itemKind]),
                resultRow[TableGrindHero.price],
                dropItemsWrapper.map {
                    HeroGrindDropItem(
                        configItemManager.getItem(it.itemId),
                        it.quantity,
                        it.weight.toFloat()
                    )
                }
            )
        }
    }
}