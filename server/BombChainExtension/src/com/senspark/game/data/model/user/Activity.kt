package com.senspark.game.data.model.user

import com.senspark.game.constant.ActionName
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import java.sql.ResultSet

class Activity(
    val id: Int,
    val type: Int,
    val itemId: Int,
    val action: Int,
    val itemName: String,
    val source: String,
    val price: Float,
    val rewardType: Int,
    val date: Long
) {
    companion object {
        fun fromResultSet(rs: ResultSet): Activity {
            return Activity(
                id = rs.getInt("instant_id"),
                type = rs.getInt("type"),
                itemId = rs.getInt("item_id"),
                action = rs.getInt("action"),
                itemName = rs.getString("item_name"),
                source = rs.getString("source"),
                price = rs.getDouble("price").toFloat(),
                rewardType = rs.getInt("reward_type"),
                date = try {
                    rs.getLong("time") * 1000L
                } catch (e: Exception) {
                    rs.getInt("time") * 1000L
                }
            )
        }
    }

    fun toSfsObject(): ISFSObject {
        val result = SFSObject()
        result.putInt("id", id)
        result.putInt("type", type)
        result.putUtfString("action", ActionName.fromValue(action).name)
        result.putUtfString("item_name", itemName)
        result.putUtfString("source", source)
        result.putInt("item_id", itemId)
        result.putDouble("price", price.toDouble())
        result.putInt("reward_type", rewardType)
        result.putLong("date", date)
        return result
    }
}