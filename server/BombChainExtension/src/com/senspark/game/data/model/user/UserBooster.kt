package com.senspark.game.data.model.user

import com.senspark.game.constant.Booster
import com.senspark.game.declare.ErrorCode
import com.senspark.game.exception.CustomException
import com.senspark.game.utils.deserializeList
import com.smartfoxserver.v2.entities.data.SFSObject
import java.sql.ResultSet
import java.time.Instant

class UserBooster(
    val listId: List<Int>,
    val itemId: Int,
    var quantity: Int
) {

    /**
     * Đánh dấu user đã chọn khi match pvp
     */
    var selected: Boolean = false
    private var dateUsed: Instant? = null

    /**
     * Đánh dấu là đã sử dụng để tính điểm rank khi kết thúc trận đấu
     */
    var used: Boolean = false
    val listUsed = ArrayList<Int>()

    companion object {
        fun fromResultSet(rs: ResultSet): UserBooster {
            return UserBooster(
                deserializeList<Int>(rs.getString("list_id")),
                rs.getInt("item_id"),
                rs.getInt("quantity")
            )
        }
    }

    fun toSfsObject(): SFSObject {
        val sfsObject = SFSObject()
        sfsObject.putInt("item_id", itemId)
        sfsObject.putInt("quantity", quantity)
        sfsObject.putInt("cool_down", Booster.fromValue(itemId).coolDown)
        sfsObject.putInt("time_effect", Booster.fromValue(itemId).timeEffect)
        return sfsObject
    }

    fun select(selected: Boolean) {
        this.selected = selected
    }

    fun applyBooster(isWhiteList: Boolean = false): Boolean {
        if (dateUsed != null && dateUsed!!.plusMillis(20000) > Instant.now()) {
            throw CustomException("Cool down invalid", ErrorCode.NOT_ENOUGH_PVP_BOOSTER)
        }
        if (!selected) throw CustomException("Booster was not selected", ErrorCode.NOT_ENOUGH_PVP_BOOSTER)
        if (Booster.fromValue(itemId).coolDown == 0 && used) throw CustomException(
            "Booster only used one time",
            ErrorCode.NOT_ENOUGH_PVP_BOOSTER
        )
        if (!isWhiteList) {
            if (!hasQuantity(1)) throw CustomException("Booster was not enough", ErrorCode.NOT_ENOUGH_PVP_BOOSTER)
            dateUsed = Instant.now()
            used = true
            quantity -= 1
            listUsed.add(listId.first { !listUsed.contains(it) })
            return true
        }
        return false
    }

    fun hasQuantity(value: Int): Boolean {
        return quantity >= value
    }

    fun clean() {
        selected = false
        used = false
    }
}