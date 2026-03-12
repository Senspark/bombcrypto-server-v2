package com.senspark.game.data.model.user

import com.senspark.game.declare.customEnum.IapStore
import com.senspark.game.declare.customEnum.SubscriptionProduct
import com.senspark.game.declare.customEnum.SubscriptionState
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import java.sql.ResultSet
import java.time.Instant

class UserSubscription(
    val uid: Int,
    val product: SubscriptionProduct,
    val startTime: Instant,
    val endTime: Instant,
    val lastModify: Instant,
    val token: String,
    val state: SubscriptionState,
    val redirect: IapStore,
) {
    companion object {
        fun fromResultSet(rs: ResultSet): UserSubscription {
            return UserSubscription(
                rs.getInt("uid"),
                SubscriptionProduct.valueOf(rs.getString("product_id")),
                Instant.ofEpochSecond(rs.getLong("start_time")),
                Instant.ofEpochSecond(rs.getLong("end_time")),
                Instant.ofEpochSecond(rs.getLong("last_modify")),
                rs.getString("token"),
                SubscriptionState.valueOf(rs.getString("state")),
                IapStore.valueOf(rs.getString("redirect")),
            )
        }
    }

    fun toSfsObject(): ISFSObject {
        return SFSObject().apply {
            putUtfString("product_id", product.normalizeName)
            putLong("start_time", startTime.epochSecond)
            putLong("end_time", endTime.epochSecond)
            putUtfString("state", state.name)
        }
    }
}