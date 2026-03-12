package com.senspark.game.data.model

import com.senspark.game.constant.OrderBy

class Filter(
    val index: Int = -1,
    private val length: Int = -1,
    val type: Int = -1,
    val itemId: Int = -1,
    val uid: Int = -1,
    val id: Int = -1,
    val status: Int = -1,
    val expirationAfter: Int = -1,
    private val sort: OrderBy = OrderBy.ModifyDateDesc
) {

    private val _sortFilter = mapOf(
        OrderBy.ModifyDateDesc to " ORDER BY unit_price DESC",
        OrderBy.ModifyDateAsc to " ORDER BY unit_price ASC",
        OrderBy.PriceDesc to " ORDER BY unit_price DESC",
        OrderBy.PriceAsc to " ORDER BY unit_price ASC",
        OrderBy.AmountAsc to " ORDER BY quantity ASC",
        OrderBy.AmountDesc to " ORDER BY quantity DESC"
    )

    fun whereStatement(): String {
        var statement = ""
        if (uid != -1) {
            statement += " AND uid_creator = $uid"
        }
        if (type != -1) {
            statement += " AND type = $type"
        }
        if (id != -1) {
            statement += " AND id = $id"
        }
        if (expirationAfter != -1) {
            statement += " AND expiration_after = $id"
        }
        return statement
    }

    fun sortStatement(): String {
        return " " + _sortFilter[sort]
    }

    fun limitStatement(): String {
        if (index != -1 && length != -1) {
            return " LIMIT $length OFFSET $index"
        }
        return ""
    }

    companion object {
        fun filterById(id: Int): Filter {
            return Filter(id = id)
        }

        fun filterByItemId(itemId: Int): Filter {
            return Filter(itemId = itemId)
        }

        fun filterByIdUser(uid: Int): Filter {
            return Filter(uid = uid)
        }

        fun all(): Filter {
            return Filter()
        }
    }
}