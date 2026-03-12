package com.senspark.game.data.model.user

import com.senspark.game.declare.customTypeAlias.ProductId
import kotlinx.serialization.Serializable
import java.sql.ResultSet
import java.time.Instant

@Serializable
class UserIAPPack(
    val uid: Int,
    val productId: ProductId,
    val saleEndDate: Long?
) {
    companion object {
        fun fromResultSet(rs: ResultSet): UserIAPPack {
            return UserIAPPack(
                rs.getInt("uid"),
                rs.getString("product_id"),
                rs.getLong("sale_end_date"),
            )
        }
    }

    fun packWasExpired() = saleEndDate?.let {
        val now = Instant.now().epochSecond
        now > it
    } ?: false
}